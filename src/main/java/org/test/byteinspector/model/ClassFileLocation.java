package org.test.byteinspector.model;

/**
 * Data structure to hold class file location
 * and how is located (is in a jar file or not).
 *
 * @author serkan
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
