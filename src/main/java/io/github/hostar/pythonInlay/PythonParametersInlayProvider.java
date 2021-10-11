package io.github.hostar.pythonInlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.util.ui.UI;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.impl.references.PyQualifiedReference;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.util.PsiTreeUtil;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class PythonParametersInlayProvider implements InlayHintsProvider<NoSettings> {
    private static final String CODE_LENS_ID = "PythonInlay";

    private static final SettingsKey<NoSettings> KEY = new SettingsKey<>(CODE_LENS_ID);

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file,
                                               @NotNull Editor editor,
                                               @NotNull NoSettings settings,
                                               @NotNull InlayHintsSink __) {
        ArrayList<ExistingInlay> existingInlaysDictionary = new ArrayList<>();

        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
                if (!hintsEnabled()) {
                    return true;
                }

                if ((element instanceof PyCallExpression)) {
                    processElement(element, sink, null);
                    return true;
                }
                if ((element instanceof PyReferenceExpression)) {
                    PsiElement newElem = PsiTreeUtil.getParentOfType(element, PyCallExpression.class);
                    if (newElem != null) {
                        processElement(newElem, sink, element);
                        return true;
                    }
                }
                return true;
            }

            private void processElement(@NotNull PsiElement element, @NotNull InlayHintsSink sink, PsiElement secondElem) {
                try {
                    PyExpression[] args = null;
                    for (PsiElement elementChildTmp : element.getChildren()) {
                        if (elementChildTmp instanceof PyArgumentList) {
                            args = ((PyArgumentList) elementChildTmp).getArguments();
                            break;
                        }
                    }

                    PsiReference referenceAt;
                    PyQualifiedReference multiRef = null;
                    if (secondElem != null) {
                        var tmp = secondElem.findReferenceAt(element.getStartOffsetInParent() + 1);
                        if (tmp instanceof PyQualifiedReference) {
                            multiRef = (PyQualifiedReference)tmp;
                        }

                        referenceAt = element.findReferenceAt(element.getStartOffsetInParent() + 1);
                    } else {
                        referenceAt = element.findReferenceAt(2);
                    }

                    if (referenceAt != null) {
                        if (secondElem != null) {
                            if (!referenceAt.getCanonicalText().equals(((PyReferenceExpression) secondElem).getName())) {
                                var tmp = element.findReferenceAt(((PyReferenceExpression) secondElem).getName().length());
                                if (tmp != null) referenceAt = tmp;
                            }
                        }

                        PsiElement elem = referenceAt.resolve();

                        if ((elem == null) && (multiRef != null)) {
                            var tmp = multiRef.multiResolve(false);
                            if (tmp.length > 0) {
                                elem = tmp[0].getElement(); // TODO: deal with multiple results
                            }
                        }

                        if ((elem != null) && (args != null)) {
                            if (elem.getContainingFile().getName().equals("builtins.pyi")) {
                                return;
                            }

                            if (elem instanceof PyFile) {
                                return;
                            }

                            for (PsiElement child : elem.getChildren()) {
                                if ((child instanceof PyParameterList)) {
                                    PyParameterList pl = (PyParameterList) child;

                                    int position = 0;
                                    int classOffset = 0;
                                    for (PyParameter parameter : pl.getParameters()) {
                                        String paramName = parameter.getName();
                                        if (paramName != null) {
                                            if (!parameter.getFirstChild().getText().startsWith("*")) {
                                                if (position == 0) {
                                                    if (((PyFunction) elem).getContainingClass() != null) {
                                                        // when class, ignore first parameter
                                                        position++;
                                                        classOffset = -1;
                                                        continue;
                                                    }
                                                }

                                                if ((position + classOffset) < args.length) {
                                                    if (validArgValue(args[position + classOffset])) {
                                                            PyExpression[] finalArgs = args;
                                                            int finalPosition = position + classOffset;

                                                            boolean match = existingInlaysDictionary.stream().anyMatch(x -> x.position == finalArgs[finalPosition].getTextOffset());

                                                            if (!match) {
                                                                addSink(sink, paramName, args[finalPosition]);
                                                                existingInlaysDictionary.add(new ExistingInlay(paramName, args[finalPosition].getTextOffset()));
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                        position++;
                                    }
                                }

                                // handle dataclass
                                if ((child instanceof PyDecoratorList)) {
                                    if (child.getText().equals("@dataclass")) {
                                        // search for PyStatementList
                                        for (PsiElement child2 : elem.getChildren()) {
                                            if ((child2 instanceof PyStatementList)) {
                                                int position = 0;
                                                for (PsiElement statement : child2.getChildren()) {
                                                    String paramName = null;
                                                    if ((statement instanceof PyTypeDeclarationStatement)) {
                                                        PyTypeDeclarationStatement pyExpressionStatement = (PyTypeDeclarationStatement)statement;
                                                        paramName = ((PyTargetExpression) pyExpressionStatement.getChildren()[0]).getName();
                                                        //paramName = pyExpressionStatement.getName();
                                                    }

                                                    if ((statement instanceof PyExpressionStatement)) {
                                                        var refExpression = statement.getChildren()[0];
                                                        if (refExpression instanceof PyReferenceExpression) {
                                                            PyReferenceExpression pyExpressionStatement = (PyReferenceExpression)refExpression;
                                                            //paramName = ((PyTargetExpression) pyExpressionStatement.getChildren()[0]).getName();
                                                            paramName = pyExpressionStatement.getName();
                                                        }
                                                    }

                                                    /* // here this is not needed, but in future it could be useful somewhere else
                                                    if ((statement instanceof PyAssignmentStatement)) {
                                                        var refExpression = statement.getChildren()[0];
                                                        if (refExpression instanceof PyTargetExpression) {
                                                            PyTargetExpression pyTargetExpression = (PyTargetExpression)refExpression;
                                                            //paramName = ((PyTargetExpression) pyExpressionStatement.getChildren()[0]).getName();
                                                            paramName = pyTargetExpression.getName();
                                                        }
                                                    }
                                                    */

                                                    if (paramName != null) {

                                                        PyExpression[] finalArgs1 = args;
                                                        int finalPosition1 = position;
                                                        boolean match = existingInlaysDictionary.stream().anyMatch(x -> x.position == finalArgs1[finalPosition1].getTextOffset());

                                                        if (!match) {
                                                            addSink(sink, paramName, args[position]);
                                                            position++;
                                                            existingInlaysDictionary.add(new ExistingInlay(paramName, finalArgs1[finalPosition1].getTextOffset()));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                }
            }

            private boolean validArgValue(PyExpression arg) {
                if (arg instanceof PyKeywordArgument) {
                    return false;
                }
                if (arg.getText().startsWith("*")) {
                    return false;
                }
                return true;
            }

            private void addSink(InlayHintsSink inlayHintsSink, String parameterName, PsiElement psiElement) {
                InlayPresentation hintTmp = getFactory().smallText(parameterName);
                InlayPresentation presentation = getFactory().roundWithBackground(hintTmp);

                inlayHintsSink.addInlineElement(psiElement.getTextOffset(), false, presentation, false);
            }
        };
    }

    static boolean hintsEnabled() {
        return InlayHintsSettings.instance().hintsEnabled(KEY, PythonLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @NotNull
    @Override
    public @Nls String getName() {
        return "Create Python inlays";
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return KEY;
    }


    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return listener -> UI.PanelFactory.panel(new JLabel()).
                withComment("This will display/hide Inlay hints for parameters for Python.").createPanel();
    }

    @Override
    public boolean isLanguageSupported(@NotNull Language language) {
        return language instanceof PythonLanguage;
    }

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

}
