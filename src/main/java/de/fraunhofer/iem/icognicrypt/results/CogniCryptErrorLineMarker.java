package de.fraunhofer.iem.icognicrypt.results;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.ClassElement;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CogniCryptErrorLineMarker implements LineMarkerProvider
{
    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement)
    {

        if (psiElement instanceof PsiStatement)
        {
            PsiStatement statement = (PsiStatement) psiElement;

            PsiClass clazz = FindClass(statement);

            boolean equals = clazz.equals("Test");

            IResultProvider resultProvider = ServiceManager.getService(psiElement.getProject(), IResultProvider.class);

            String qualifiedName = clazz.getQualifiedName();

            int lineNumber = getLineNumber(psiElement) + 1;
            //Check if an error exists for the line number that the element is located

            String path = psiElement.getContainingFile().getVirtualFile().getPath();

            Set<CogniCryptError> errors = resultProvider.FindErrors(path, lineNumber);
            if (!errors.isEmpty())
                return CreateNewMarker(psiElement, errors);
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> list, @NotNull Collection<LineMarkerInfo> collection) {
        int i = 0;
    }

    private LineMarkerInfo CreateNewMarker(PsiElement psiElement, Iterable<CogniCryptError> errors){
        return new LineMarkerInfo<>(psiElement, psiElement.getTextRange(), PluginIcons.ERROR, Pass.EXTERNAL_TOOLS,
                new TooltipProvider(getErrorsMessage(errors)), null, GutterIconRenderer.Alignment.LEFT);
    }

    private String getErrorsMessage(Iterable<CogniCryptError> errors) {
        String s = "";
        for(CogniCryptError e : errors){
            s += e.getErrorMessage() +"\n";
        }
        return s;
    }

    //Returns line number for PSI element
    private int getLineNumber(PsiElement psiElement) {
        PsiFile containingFile = psiElement.getContainingFile();
        Project project = containingFile.getProject();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        Document document = psiDocumentManager.getDocument(containingFile);
        int textOffset = psiElement.getTextOffset();

        return document.getLineNumber(textOffset);
    }

    private PsiClass FindClass(PsiElement element){
        if (element instanceof PsiClass)
            return (PsiClass) element;
        if (element == null)
            return null;
        return FindClass(element.getParent());
    }
}
