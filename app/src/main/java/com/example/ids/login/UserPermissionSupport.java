package com.example.ids.login;

public class UserPermissionSupport {

    private boolean userPermission;
    private boolean guidePermission;
    private boolean adminPermission;

    public UserPermissionSupport() {
        userPermission = false;
        guidePermission = false;
        adminPermission = false;
    }

    public void setUserPermission() {
        userPermission = true;
        guidePermission = false;
        adminPermission = false;
    }

    public void setGuidePermission() {
        userPermission = true;
        guidePermission = true;
        adminPermission = false;
    }

    public void setAdminPermission() {
        userPermission = true;
        guidePermission = true;
        adminPermission = true;
    }

    public boolean checkUserPermission() {
        return userPermission;
    }

    public boolean checkGuidePermission() {
        return guidePermission;
    }

    public boolean checkAdminPermission() {
        return adminPermission;
    }

}
