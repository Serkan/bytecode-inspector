package org.test.byteinspector.model;

/**
 * Created by serkan on 01.01.2015.
 */
public class ClassFileLocation {

    private boolean isZip;

    private String zipName;

    private String fileName;

    public boolean isZip() {
        return isZip;
    }

    public void setZip(boolean isZip) {
        this.isZip = isZip;
    }

    public String getZipName() {
        return zipName;
    }

    public void setZipName(String zipName) {
        this.zipName = zipName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
