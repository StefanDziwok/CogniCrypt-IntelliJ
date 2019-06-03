package de.fraunhofer.iem.icognicrypt.analysis;

import com.android.tools.idea.gradle.project.build.GradleBuildContext;
import com.android.tools.idea.gradle.project.build.invoker.GradleInvocationResult;
import com.android.tools.idea.project.AndroidProjectBuildNotifications;
import com.google.common.base.Joiner;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import de.fraunhofer.iem.icognicrypt.Constants;
import de.fraunhofer.iem.icognicrypt.actions.IcognicryptSettings;
import de.fraunhofer.iem.icognicrypt.ui.SettingsDialog;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CompilationListener implements ProjectComponent {

    private MessageBusConnection connection;
    private final Project project;
    private static final Logger logger = LoggerFactory.getLogger(CompilationListener.class);

    public CompilationListener(Project project) {
        this.project = project;
    }

    @Override
    public void initComponent() {

        logger.info("Initializing ICogniCrypt");

        if(!Constants.AUTOMATIC_SCAN_ON_COMPILE){
            return;
        }

        /*
            Listens for Build notifications in Android Studio.
        */
        AndroidProjectBuildNotifications.subscribe(project, context -> {

            logger.info("Call Source {} buildComplete", project);

            if (context instanceof GradleBuildContext) {

                GradleBuildContext gradleBuildContext = (GradleBuildContext) context;
                GradleInvocationResult build = gradleBuildContext.getBuildResult();

                if (build.isBuildSuccessful()) {

                    for (String buildTask : build.getTasks()) {

                        if (buildTask.equals("clean"))
                            continue;

                        startAnalyser(Constants.IDE_ANDROID_STUDIO, project);
                    }
                }
            }
        });


        /*
            Listens for Build notifications in IntelliJ.
        */
        connection = project.getMessageBus().connect();
        connection.subscribe(CompilerTopics.COMPILATION_STATUS, new CompilationStatusListener() {

            @Override
            public void compilationFinished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                logger.info("Call Source {} compilationFinished");

                if (!aborted)
                    startAnalyser(Constants.IDE_INTELLIJ, compileContext.getProject());
            }

            @Override
            public void automakeCompilationFinished(int errors, int warnings, CompileContext compileContext) {

                startAnalyser(Constants.IDE_INTELLIJ, compileContext.getProject());
            }
        });
    }

    public static void startAnalyser(int IDE, Project project) {

        String modulePath = "";
        List<String> classpath = new ArrayList<>();

        //Get modules that are present for each project
        //Get output path for IntelliJ project or APK path in Android Studio
        switch (IDE) {
            case Constants.IDE_ANDROID_STUDIO:

                String path = project.getBasePath();
                logger.info("Evaluating compile path {}", path);

                File apkDir = new File(path);

                if (apkDir.exists()) {

                    for (File file : FileUtils.listFiles(apkDir, new String[]{"apk"}, true)) {

                        logger.info("Evaluating file {}", file.getName());
                        modulePath = file.getAbsolutePath();
                        logger.info("APK found in {} ", modulePath);

                        String android_sdk_root = System.getenv("ANDROID_HOME");
                        if (android_sdk_root == null || "".equals(android_sdk_root)) {
                            throw new RuntimeException("Environment variable ANDROID_HOME not set!");
                        }
                        Runnable analysis = new AndroidProjectAnalysis(modulePath, android_sdk_root + File.separator+"platforms", getRulesDirectory());
                        ProgressManager.getInstance().runProcess(analysis, new ProgressIndicator() {
                            @Override
                            public void start() {

                            }

                            @Override
                            public void stop() {

                            }

                            @Override
                            public boolean isRunning() {
                                return false;
                            }

                            @Override
                            public void cancel() {

                            }

                            @Override
                            public boolean isCanceled() {
                                return false;
                            }

                            @Override
                            public void setText(String text) {

                            }

                            @Override
                            public String getText() {
                                return null;
                            }

                            @Override
                            public void setText2(String text) {

                            }

                            @Override
                            public String getText2() {
                                return null;
                            }

                            @Override
                            public double getFraction() {
                                return 0;
                            }

                            @Override
                            public void setFraction(double fraction) {

                            }

                            @Override
                            public void pushState() {

                            }

                            @Override
                            public void popState() {

                            }

                            @Override
                            public boolean isModal() {
                                return false;
                            }

                            @NotNull
                            @Override
                            public ModalityState getModalityState() {
                                return null;
                            }

                            @Override
                            public void setModalityProgress(ProgressIndicator modalityProgress) {

                            }

                            @Override
                            public boolean isIndeterminate() {
                                return false;
                            }

                            @Override
                            public void setIndeterminate(boolean indeterminate) {

                            }

                            @Override
                            public void checkCanceled() throws ProcessCanceledException {

                            }

                            @Override
                            public boolean isPopupWasShown() {
                                return false;
                            }

                            @Override
                            public boolean isShowing() {
                                return false;
                            }
                        });
                    }
                }

                break;
            case Constants.IDE_INTELLIJ:

                for (Module module : ModuleManager.getInstance(project).getModules()) {

                    logger.info("Checking {} module, type={}", module.getName(), module.getModuleTypeName());

                    //Add classpath jars from Java, Android and Gradle to list
                    for (VirtualFile file : OrderEnumerator.orderEntries(module).recursively().getClassesRoots()) {
                        classpath.add(file.getPath());
                    }
                    modulePath = CompilerPathsEx.getModuleOutputPath(module, false);
                    logger.info("Module Output Path {} ", modulePath);
                    Runnable analysis = new JavaProjectAnalysis(modulePath, Joiner.on(":").join(classpath), getRulesDirectory());
                    ProgressManager.getInstance().executeNonCancelableSection(analysis);
                }
                break;
        }

    }

    public static String getRulesDirectory() {

        IcognicryptSettings settings = IcognicryptSettings.getInstance();
        File rules = new File(settings.getRulesDirectory());

        //TODO Check if directory contains correct files
        if (rules.exists())
            return settings.getRulesDirectory();
        else {
            SettingsDialog settingsDialog = new SettingsDialog();
            settingsDialog.pack();
            settingsDialog.setSize(550, 150);
            settingsDialog.setLocationRelativeTo(null);
            settingsDialog.setVisible(true);

            return settings.getRulesDirectory();
        }
    }

    public void disposeComponent() {
        connection.disconnect();
    }
}
