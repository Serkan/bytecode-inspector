package org.test.byteinspector.model;

/**
 * Checked exception to represent error in statistics calculation.
 *
 * @author serkan
 */
public class CalculationException extends Exception {

    /**
     * Default constructor.
     *
     * @param s exception message to pass
     */
    public CalculationException(String s) {
        super(s);
    }

}
