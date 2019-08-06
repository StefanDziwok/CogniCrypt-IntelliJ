package de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Outputs;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import de.fraunhofer.iem.icognicrypt.IdeSupport.gradle.GradleSettings;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.IdeaWorkspace;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.JavaModule;
import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.ProjectModuleManager;
import de.fraunhofer.iem.icognicrypt.core.Dialogs.DialogHelper;
import de.fraunhofer.iem.icognicrypt.exceptions.CogniCryptException;
import de.fraunhofer.iem.icognicrypt.settings.IPersistableCogniCryptSettings;
import org.apache.commons.lang.NotImplementedException;

import javax.naming.OperationNotSupportedException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

class AndroidStudioOutputFinder implements IOutputFinderInternal
{
    private static final Logger logger = Logger.getInstance(AndroidStudioOutputFinder.class);
    private final IPersistableCogniCryptSettings _settings;

    AndroidStudioOutputFinder()
    {
        _settings = ServiceManager.getService(IPersistableCogniCryptSettings.class);
    }

    @Override
    public Iterable<File> GetOutputFiles(Project project) throws OperationNotSupportedException, IOException, CogniCryptException
    {
        return GetOutputFiles(project, _settings.GetFindOutputOptions());
    }

    @Override
    public Iterable<File> GetOutputFiles(Project project, EnumSet<OutputFinderOptions.Flags> options) throws CogniCryptException, IOException, OperationNotSupportedException
    {
        Path path = Paths.get(project.getBasePath());
        return GetOutputFiles(path, options);
    }

    @Override
    public Iterable<File> GetOutputFiles(Path projectPath, EnumSet<OutputFinderOptions.Flags> options) throws CogniCryptException, IOException, OperationNotSupportedException
    {
        logger.info("Try finding all built .apk files with options: " + options);

        if (!Files.exists(projectPath))
            throw new CogniCryptException("Root path of the project does not exist.");

        HashSet<File> result = new HashSet<>();

        if (!options.isEmpty()){
            result.addAll(GetModuleOutputs(projectPath, options));
            result.addAll(GetExportedOutputs(projectPath, options));
        }

        if (result.isEmpty())
        {
            logger.info("Could not find any file. User is requested to choose one manually");
            FileFilter filter = new FileNameExtensionFilter("Android Apps", "apk");
            File userSelectedFile = DialogHelper.ChooseSingleFileFromDialog("Choose an .apk File to analyze...", filter, projectPath);
            if (userSelectedFile == null) logger.info("User did not select any file.");
            else
            {
                logger.info("Added manual file: " + userSelectedFile.getAbsolutePath());
                result.add(userSelectedFile);
            }
        }

        return result;
    }

    private Collection<File> GetExportedOutputs(Path projectRootPath, EnumSet<OutputFinderOptions.Flags> options) throws IOException
    {
        logger.info("Get exported .apks from workspace cache");

        File workspaceFile = Paths.get(projectRootPath.toString(), ".idea\\workspace.xml").toFile();

        try
        {
            IdeaWorkspace workspace = new IdeaWorkspace(workspaceFile);
            return GetOutputs(workspace.GetOutputManager(), options);
        }
        catch (FileNotFoundException e)
        {
            return Collections.EMPTY_LIST;
        }
    }

    private Collection<File> GetModuleOutputs(Path projectRootPath, EnumSet<OutputFinderOptions.Flags> options) throws IOException, OperationNotSupportedException
    {
        logger.info("Get .apks from project modules");
        GradleSettings settings = new GradleSettings(projectRootPath);
        ProjectModuleManager moduleManager = new ProjectModuleManager(settings);

        HashSet<File> result = new HashSet<>();

        if (OutputFinderOptions.contains(options, OutputFinderOptions.Flags.SignedOnly))
            return result;

        for (JavaModule module : moduleManager.GetModules())
        {
            result.addAll(GetOutputs(module.GetOutputManager(), options));
        }
        return result;
    }

    private Collection<File> GetOutputs(IHasOutputs outputManager, EnumSet<OutputFinderOptions.Flags> options) throws IOException
    {
        HashSet<File> result = new HashSet<>();
        for (String output : outputManager.GetOutputs(options))
        {
            File file = new File(output);
            if (file.exists())
            {
                result.add(file);
                logger.info("Found .apk File: " + file.getCanonicalPath());
            }
        }
        return result;
    }
}

