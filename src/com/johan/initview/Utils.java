package com.johan.initview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    // view id 正则
    private static final Pattern idPattern = Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE);

    /**
     * 从ViewId（@+id/view_id）获取Id（view_id）
     * @param viewId
     * @return
     */
    public static String getViewId(String viewId) {
        Matcher matcher = idPattern.matcher(viewId);
        if (matcher.find() && matcher.groupCount() > 1) {
            return matcher.group(2);
        }
        return null;
    }

    /**
     * 获取布局名（@layout/layoutName -> layoutName）
     * @param layout
     * @return
     */
    private static String getLayoutName(String layout) {
        if (layout == null || !layout.startsWith("@") || !layout.contains("/")) return null;
        String[] parts = layout.split("/");
        if (parts.length != 2) return null;
        return parts[1];
    }

    /**
     * 解析xmlLayoutFile布局文件，结果保存elementList中
     * @param xmlLayoutFile
     * @param elementList
     */
    public static void parseXmlLayout(final PsiFile xmlLayoutFile, final List<Element> elementList) {
        String fileName = xmlLayoutFile.getName();
        int index = fileName.indexOf("_");
        if (index != -1) {
            fileName = fileName.substring(index + 1);
        }
        index = fileName.indexOf(".");
        if (index != -1) {
            fileName = fileName.substring(0, index);
        }
        final String prefix = fileName;
        xmlLayoutFile.accept(new XmlRecursiveElementVisitor(){
            @Override
            public void visitElement(PsiElement element) {
                super.visitElement(element);
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName();
                    // 如果是include标签，暂时不要找include标签
                    /* if (name.equalsIgnoreCase("include")) {
                        XmlAttribute layoutAttr = tag.getAttribute("layout", null);
                        if (layoutAttr == null) return;
                        String layout = getLayoutName(layoutAttr.getValue());
                        if (layout == null) return;
                        // 再次查找include的layout布局文件
                        Project project = xmlLayoutFile.getProject();
                        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, layout + ".xml", GlobalSearchScope.allScope(project));
                        if (psiFiles == null || psiFiles.length == 0) return;
                        XmlFile layoutFile = (XmlFile) psiFiles[0];
                        // 递归查找
                        parseXmlLayout(layoutFile, elementList);
                        return;
                    } */
                    // 如果不是include，那么就是view标签
                    String id = "";
                    XmlAttribute idAttr = tag.getAttribute("android:id", null);
                    if (idAttr != null) {
                        id = getViewId(idAttr.getValue());
                    }
                    XmlAttribute clickableAttr = tag.getAttribute("android:clickable", null);
                    boolean clickable = clickableAttr == null ? false : clickableAttr.getValue().equals("false") ? false : true;
                    XmlAttribute onClickAttr = tag.getAttribute("android:onClick", null);
                    String onClick = onClickAttr== null ? null : onClickAttr.getValue();
                    Element parseResultElement = new Element(prefix, name, id, clickable, onClick);
                    elementList.add(parseResultElement);
                }
            }
        });
    }

    /**
     * 从当前文件获取class文件
     * @param editor
     * @param file
     * @return
     */
    public static PsiClass getTargetClass(Editor editor, PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        } else {
            PsiClass target = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            return target instanceof SyntheticElement ? null : target;
        }
    }

}
