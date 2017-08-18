package com.johan.initview;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlFile;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InitViewAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        // 获取project
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        // 获取选中内容
        final Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        String selectedText = editor.getSelectionModel().getSelectedText();
        // 如果没有选中内容，提示选择布局
        if (StringUtils.isEmpty(selectedText) || !StringUtils.startsWith(selectedText,"R.layout.")) {
            ViewUtils.showPopupBalloon(editor, "请选择布局id");
            return;
        }
        String layoutFileName = selectedText.substring(9) + ".xml";
        // 通过文件名查找布局文件
        PsiFile[] getFiles = FilenameIndex.getFilesByName(project, layoutFileName, GlobalSearchScope.allScope(project));
        // 如果没有找到，提示没有找到布局文件
        if (getFiles == null || getFiles.length == 0) {
            ViewUtils.showPopupBalloon(editor, "没有找到布局文件：" + layoutFileName);
            return;
        }
        // 解析XML
        XmlFile layoutFile = (XmlFile) getFiles[0];
        List<Element> elementList = new ArrayList<>();
        Utils.parseXmlLayout(layoutFile, elementList);
        if (elementList.size() == 0) {
            ViewUtils.showPopupBalloon(editor, "没有找到任何id");
            return;
        }
        // 写入文件，不允许在主线程中进行实时的文件写入
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        PsiClass psiClass = Utils.getTargetClass(editor, psiFile);
        InitViewCreator creator = new InitViewCreator(project, psiFile, psiClass, elementList);
        creator.execute();
    }

}
