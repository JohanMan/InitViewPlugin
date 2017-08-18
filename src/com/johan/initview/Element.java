package com.johan.initview;

public class Element {

    // view id
    private String id;
    // view name 如TextView
    private String name;
    // clickable
    private boolean clickable = false;
    // on click function
    private String onClick;

    public Element(String name, String id, boolean clickable, String onClick) {
        String[] packages = name.split("\\.");
        if (packages.length > 1) {
            // com.example.CustomView
            this.name = packages[packages.length - 1];
        } else {
            this.name = name;
        }
        this.id = id;
        this.clickable = clickable;
        this.onClick = onClick;
    }

    public String getName() {
        return name;
    }

    public boolean isClickable() {
        return clickable;
    }

    public String getOnClick() {
        return onClick;
    }

    /**
     * 获取id，R.id.id
     * @return
     */
    public String getFullId() {
        return "R.id." + id;
    }

    /**
     * 获取变量名
     * @return
     */
    public String getFieldName() {
        if (id.indexOf("_") == -1) {
            return id + getFieldNameSuffix();
        }
        StringBuilder fieldNameBuilder = new StringBuilder();
        String[] ids = id.split("_");
        for (int i = 0; i < ids.length; i++) {
            if (i == 0) {
                fieldNameBuilder.append(ids[i]);
            } else {
                fieldNameBuilder.append(toFirstUpper(ids[i]));
            }
        }
        fieldNameBuilder.append(getFieldNameSuffix());
        return fieldNameBuilder.toString();
    }

    /**
     * 第一个变大写
     * @param name
     * @return
     */
    private String toFirstUpper(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * 个人习惯，命名view的变量时加入View类型作为后缀
     * @return
     */
    private String getFieldNameSuffix() {
        if (name.endsWith("Layout")) {
            return "Layout";
        } else if (name.endsWith("Button")) {
            return "Button";
        } else if (name.endsWith("Bar")) {
            return "Bar";
        } else if (name.endsWith("Spinner")) {
            return "Spinner";
        } else if (name.endsWith("Box")) {
            return "Box";
        } else {
            return "View";
        }
    }

}
