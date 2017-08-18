package com.johan.initview;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.List;

public class InitViewCreator extends WriteCommandAction.Simple {

    private List<Element> elementList;
    private Project project;
    private PsiFile psiFile;
    private PsiClass psiClass;
    private PsiElementFactory factory;

    protected InitViewCreator(Project project, PsiFile psiFile, PsiClass psiClass, List<Element> elementList) {
        super(project, psiFile);
        this.project = project;
        this.psiFile = psiFile;
        this.psiClass = psiClass;
        this.factory = JavaPsiFacade.getElementFactory(project);
        this.elementList = elementList;
    }

    @Override
    protected void run() throws Throwable {
        buildViewField();
        buildInitViewMethod();
        buildOnClickMethod();
        if (buildClickableMethod()) {
            importOnClickListener();
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
        styleManager.optimizeImports(psiFile);
        styleManager.shortenClassReferences(psiClass);
        new ReformatCodeProcessor(project, psiClass.getContainingFile(), null, false).runWithoutProgress();
    }

    /**
     * 创建View字段
     */
    private void buildViewField() {
        for (Element element : elementList) {
            if (element.getOnClick() != null) continue;
            if (hasField(element.getFieldName())) continue;
            String field = "private " + element.getName() + " " + element.getFieldName() + ";";
            // 写入字段
            psiClass.add(factory.createFieldFromText(field, psiClass));
        }
    }

    /**
     * 判断是否存在field字段
     * @param field
     * @return
     */
    private boolean hasField(String field) {
        // 获取这个类所有的字段
        PsiField[] psiFields = psiClass.getAllFields();
        for (PsiField psiField : psiFields) {
            if (psiField.getName().equals(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建initView方法
     */
    private void buildInitViewMethod() {
        PsiMethod psiMethod = getMethod("initView");
        // 如果initView方法已经存在
        if (psiMethod != null) {
            for (Element element : elementList) {
                if (element.getOnClick() != null) continue;
                // 写入初始化view
                String insertCode = element.getFieldName() + " = (" + element.getName() + ")" + " findViewById(" + element.getFullId() + ");";
                if (!containTextInMethod(psiMethod, insertCode)) {
                    // 在方法内写入insertCode语句
                    psiMethod.getBody().add(factory.createStatementFromText(insertCode, psiClass));
                }
                // 如果设置clickable，写入setOnClickListener
                if (!element.isClickable()) continue;
                insertCode = element.getFieldName() + ".setOnClickListener(this);";
                if (!containTextInMethod(psiMethod, insertCode)) {
                    // 在方法内写入insertCode语句
                    psiMethod.getBody().add(factory.createStatementFromText(insertCode, psiClass));
                }
            }
            return;
        }
        // initView方法不存在
        StringBuilder methodBuilder = new StringBuilder();
        methodBuilder.append("private void initView() {").append("\n");
        for (Element element : elementList) {
            if (element.getOnClick() != null) continue;
            methodBuilder.append(element.getFieldName()).append(" = ").append("(").append(element.getName()).append(")")
                    .append(" findViewById(").append(element.getFullId()).append(");").append("\n");
            if (!element.isClickable()) continue;
            methodBuilder.append(element.getFieldName()).append(".setOnClickListener(this);\n");
        }
        methodBuilder.append("}");
        // 写入方法
        psiClass.add(factory.createMethodFromText(methodBuilder.toString(), psiClass));
    }

    /**
     * 获取方法
     * @param method
     * @return
     */
    private PsiMethod getMethod(String method) {
        // 获取类所有方法
        PsiMethod[] psiMethods = psiClass.getAllMethods();
        for (PsiMethod psiMethod : psiMethods) {
            if (psiMethod.getName().equals(method)) {
                return psiMethod;
            }
        }
        return null;
    }

    /**
     * 判断method方法内是否包含text内容
     * @param method
     * @param text
     * @return
     */
    private boolean containTextInMethod(PsiMethod method, String text) {
        PsiCodeBlock codeBlock = method.getBody();
        PsiStatement[] statements = codeBlock.getStatements();
        for (PsiStatement statement : statements) {
            if (statement.getText().contains(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建布局文件制定OnClick方法
     */
    private void buildOnClickMethod() {
        for (Element element : elementList) {
            if (element.getOnClick() == null) continue;
            if (getMethod(element.getOnClick()) != null) continue;
            String insertMethod = "public void " + element.getOnClick() + " (View view) {\n\n}\n";
            // 写入方法
            psiClass.add(factory.createMethodFromText(insertMethod, psiClass));
        }
    }

    /**
     * 创建onClick方法
     * @return
     */
    private boolean buildClickableMethod() {
        PsiMethod psiMethod = getMethod("onClick");
        // 如果onClick方法已经存在
        if (psiMethod != null) {
            for (Element element : elementList) {
                if (element.getOnClick() != null) continue;
                // 如果同时设置onClick和clickable，clickable不起作用
                if (!element.isClickable()) continue;
                String insertCode = "case " + element.getFullId() + ":";
                if (containTextInMethod(psiMethod, insertCode)) continue;
                // 找到第一个switch块
                PsiSwitchStatement switchStatement = findSwitchStatement(psiMethod);
                if (switchStatement == null) {
                    // 如果没有找到switch块，写入switch块
                    psiMethod.getBody().add(factory.createStatementFromText("switch(view.getId()) {\n" + insertCode + "\nbreak;\n}", psiClass));
                    continue;
                }
                // 在switch块内写入case
                switchStatement.getBody().add(factory.createStatementFromText(insertCode, psiClass));
                switchStatement.getBody().add(factory.createStatementFromText("break;", psiClass));
            }
            return false;
        }
        // onClick方法不存在
        StringBuilder methodBuilder = new StringBuilder();
        methodBuilder.append("@Override\n");
        methodBuilder.append("public void onClick(View view) {").append("\n");
        methodBuilder.append("switch(view.getId()) {");
        boolean hasClickable = false;
        for (Element element : elementList) {
            if (element.getOnClick() != null) continue;
            if (!element.isClickable()) continue;
            hasClickable = true;
            methodBuilder.append("case ").append(element.getFullId()).append(":\nbreak;\n");
        }
        methodBuilder.append("}");
        // 判断是否设置了点击事件(clickable)
        if (hasClickable) {
            //写入方法
            psiClass.add(factory.createMethodFromText(methodBuilder.toString(), psiClass));
        }
        return hasClickable;
    }

    /**
     * 查找method方法内的switch
     * @param method
     * @return
     */
    private PsiSwitchStatement findSwitchStatement(PsiMethod method) {
        PsiCodeBlock codeBlock = method.getBody();
        PsiStatement[] statements = codeBlock.getStatements();
        for (PsiStatement statement : statements) {
            if (statement instanceof PsiSwitchStatement) {
                PsiSwitchStatement switchStatement = (PsiSwitchStatement) statement;
                return switchStatement;
            }
        }
        return null;
    }

    /**
     * 类实现OnClickListener接口
     */
    private void importOnClickListener() {
        // 获取所有实现的接口
        PsiReferenceList implementsList = psiClass.getImplementsList();
        boolean isImplOnClick = false;
        if (implementsList != null) {
            PsiJavaCodeReferenceElement[] referenceElements = implementsList.getReferenceElements();
            isImplOnClick = isImplementsOnClickListener(referenceElements);
        }
        if (!isImplOnClick) {
            // 加入OnClickListener接口
            PsiJavaCodeReferenceElement referenceElementByFQClassName =
                    factory.createReferenceElementByFQClassName("android.view.View.OnClickListener", psiClass.getResolveScope());
            if (implementsList != null) {
                implementsList.add(referenceElementByFQClassName);
            }
        }
    }

    /**
     * 判断是否实现OnClickListener接口
     * @param referenceElements
     * @return
     */
    private boolean isImplementsOnClickListener(PsiJavaCodeReferenceElement[] referenceElements) {
        for (PsiJavaCodeReferenceElement referenceElement : referenceElements) {
            if (referenceElement.getText().contains("OnClickListener")) {
                return true;
            }
        }
        return false;
    }

}
