package de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Outputs;

import com.intellij.openapi.project.Project;
import de.fraunhofer.iem.icognicrypt.exceptions.CogniCryptException;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

interface IOutputFinderInternal
{
    @NotNull Iterable<File> GetOutputFiles(Project project) throws OperationNotSupportedException, IOException, CogniCryptException;

    @NotNull Iterable<File> GetOutputFiles(Project project, Set<OutputFinderOptions.Flags> options) throws CogniCryptException, IOException, OperationNotSupportedException;

    @NotNull Iterable<File> GetOutputFilesFromDialog(Project project);
}
