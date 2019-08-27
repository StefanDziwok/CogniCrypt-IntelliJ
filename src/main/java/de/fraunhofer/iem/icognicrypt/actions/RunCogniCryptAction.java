package de.fraunhofer.iem.icognicrypt.actions;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import de.fraunhofer.iem.crypto.CogniCryptAndroidAnalysis;
import de.fraunhofer.iem.icognicrypt.IdeSupport.platform.IIdePlatformProvider;
import de.fraunhofer.iem.icognicrypt.IdeSupport.platform.IdeType;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Outputs.IProjectOutputFinder;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Outputs.OutputFinderOptions;
import de.fraunhofer.iem.icognicrypt.analysis.CogniCryptAndroidStudioAnalysisTask;
import de.fraunhofer.iem.icognicrypt.analysis.JavaProjectAnalysisTask;
import de.fraunhofer.iem.icognicrypt.core.Collections.Linq;
import de.fraunhofer.iem.icognicrypt.core.android.AndroidPlatformLocator;
import de.fraunhofer.iem.icognicrypt.core.crySL.CrySLHelper;
import de.fraunhofer.iem.icognicrypt.settings.IPersistableCogniCryptSettings;
import de.fraunhofer.iem.icognicrypt.ui.NotificationProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

public class RunCogniCryptAction extends CogniCryptAction implements DumbAware
{
    private static final Logger logger = Logger.getInstance(RunCogniCryptAction.class);
    private final IPersistableCogniCryptSettings _settings;
    private final IdeType _ideType;

    public RunCogniCryptAction()
    {
        super("CogniCrypt Analysis", "Run CogniCrypt Analysis", IconLoader.getIcon("/icons/cognicrypt.png"));
        _settings = ServiceManager.getService(IPersistableCogniCryptSettings.class);
        _ideType = ServiceManager.getService(IIdePlatformProvider.class).GetRunningPlatform();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e)
    {
        Project project = e.getDataContext().getData(PlatformDataKeys.PROJECT);

        if (!CrySLHelper.isValidCrySLRuleDirectory(_settings.getRulesDirectory()))
        {
            NotificationProvider.Warn("No valid CrySL rules found. Please go to \"File > Settings > Other Settings > CogniCrypt\" and select a valid directory.");
            return;
        }

        Iterable<File> files = GetFilesToAnalyze(project);
        if (files == null || !Linq.any(files))
        {
            NotificationProvider.ShowInfo("No files were analysed");
            return;
        }

        switch (_ideType)
        {
            case IntelliJ:
                RunIntelliJAnalysis(project, files);
                break;
            case AndroidStudio:
                RunAndroidAnalysis(project, files);
                break;
            default:
                NotificationProvider.ShowError("Could not run the Analysis on the current IDE platform");
                break;
        }
    }

    private void RunAndroidAnalysis(Project project, Iterable<File> filesToAnalyze)
    {
        logger.info("Running Android Analysis");

        Path androidPlatformPath = AndroidPlatformLocator.getAndroidPlatformsLocation(project).toAbsolutePath();
        String projectBasePath = project.getBasePath();

        logger.info("Evaluating compile path " + projectBasePath);

        File projectDir = new File(projectBasePath);
        if (!projectDir.exists()) return;

        LinkedList<CogniCryptAndroidAnalysis> queue = Lists.newLinkedList();

        for (File file : filesToAnalyze)
        {
            String apkPath = file.getAbsolutePath();
            logger.info("APK found in " + apkPath);

            CogniCryptAndroidAnalysis analysis = new CogniCryptAndroidAnalysis(apkPath, androidPlatformPath.toString(), _settings.getRulesDirectory(), Lists.newArrayList());
            queue.add(analysis);
        }
        if (queue.isEmpty()) NotificationProvider.ShowInfo("No APK file detected. Run Build > Make Project assemble an APK and trigger the analysis again.");
        else ProgressManager.getInstance().run(new CogniCryptAndroidStudioAnalysisTask(project, queue));
    }

    private void RunIntelliJAnalysis(Project project, Iterable<File> filesToAnalyze)
    {
        logger.info("Running IntelliJ Analysis");

        List<String> classpath = new ArrayList<>();
        for (Module module : ModuleManager.getInstance(project).getModules())
        {
            //Add classpath jars from Language, Android and Gradle to list
            for (VirtualFile file : OrderEnumerator.orderEntries(module).recursively().getClassesRoots())
            {
                classpath.add(file.getPath());
            }
            String modulePath = CompilerPathsEx.getModuleOutputPath(module, false);
            logger.info("Module Output Path " + modulePath);
            Task analysis = new JavaProjectAnalysisTask(modulePath, Joiner.on(":").join(classpath), _settings.getRulesDirectory());
            ProgressManager.getInstance().run(analysis);
        }
    }

    private Iterable<File> GetFilesToAnalyze(Project project){
        IProjectOutputFinder outputFinder = ServiceManager.getService(project, IProjectOutputFinder.class);

        EnumSet<OutputFinderOptions.Flags> options = _settings.GetFindOutputOptions();

        if (options == null || !Linq.any(options))
            return outputFinder.GetOutputFilesFromDialog();

        Iterable<File> files = outputFinder.GetOutputFiles(options);

        if (files == null || Linq.count(files))
            return null;

        if (!Linq.any(files) && Linq.any(options))
        {
            logger.info("No .apk file was found. User is requested to choose one manually");
            files = outputFinder.GetOutputFilesFromDialog();
        }
        return files;
    }
}


