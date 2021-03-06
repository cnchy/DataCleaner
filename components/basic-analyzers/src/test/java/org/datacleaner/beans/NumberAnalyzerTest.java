/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Free Software Foundation, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.beans;

import org.datacleaner.data.MockInputColumn;
import org.datacleaner.data.MockInputRow;
import org.datacleaner.result.CrosstabResult;
import org.datacleaner.result.renderer.CrosstabTextRenderer;

import junit.framework.TestCase;

public class NumberAnalyzerTest extends TestCase {

    private MockInputColumn<Float> col1;
    private MockInputColumn<Long> col2;
    private MockInputColumn<Byte> col3;
    private NumberAnalyzer numberAnalyzer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        col1 = new MockInputColumn<>("foo", Float.class);
        col2 = new MockInputColumn<>("bar", Long.class);
        col3 = new MockInputColumn<>("w00p", Byte.class);

        numberAnalyzer = new NumberAnalyzer(col1, col2, col3);
    }

    public void testNoRows() throws Exception {
        final CrosstabResult result = numberAnalyzer.getResult();
        final String[] resultLines = new CrosstabTextRenderer().render(result).split("\n");
        assertEquals(12, resultLines.length);
        assertEquals("                      foo    bar   w00p ", resultLines[0]);
        assertEquals("Row count               0      0      0 ", resultLines[1]);
        assertEquals("Null count              0      0      0 ", resultLines[2]);
        assertEquals("Highest value      <null> <null> <null> ", resultLines[3]);
        assertEquals("Lowest value       <null> <null> <null> ", resultLines[4]);
        assertEquals("Sum                <null> <null> <null> ", resultLines[5]);
        assertEquals("Mean               <null> <null> <null> ", resultLines[6]);
        assertEquals("Geometric mean     <null> <null> <null> ", resultLines[7]);
        assertEquals("Standard deviation <null> <null> <null> ", resultLines[8]);
        assertEquals("Variance           <null> <null> <null> ", resultLines[9]);
        assertEquals("Second moment      <null> <null> <null> ", resultLines[10]);
        assertEquals("Sum of squares     <null> <null> <null> ", resultLines[11]);
    }

    public void testOnlyeOneColumnNonNull() throws Exception {
        numberAnalyzer.run(new MockInputRow().put(col2, 1L), 1);
        numberAnalyzer.run(new MockInputRow().put(col2, 2L), 1);
        numberAnalyzer.run(new MockInputRow().put(col2, 3L), 1);
        numberAnalyzer.run(new MockInputRow().put(col2, 4L), 1);
        numberAnalyzer.run(new MockInputRow().put(col2, 5L), 1);

        final CrosstabResult result = numberAnalyzer.getResult();
        final String[] resultLines = new CrosstabTextRenderer().render(result).split("\n");
        assertEquals(12, resultLines.length);
        assertEquals("                      foo    bar   w00p ", resultLines[0]);
        assertEquals("Row count               5      5      5 ", resultLines[1]);
        assertEquals("Null count              5      0      5 ", resultLines[2]);
        assertEquals("Highest value      <null>      5 <null> ", resultLines[3]);
        assertEquals("Lowest value       <null>      1 <null> ", resultLines[4]);
        assertEquals("Sum                <null>     15 <null> ", resultLines[5]);
        assertEquals("Mean               <null>      3 <null> ", resultLines[6]);
        assertEquals("Geometric mean     <null>   2.61 <null> ", resultLines[7]);
        assertEquals("Standard deviation <null>   1.58 <null> ", resultLines[8]);
        assertEquals("Variance           <null>    2.5 <null> ", resultLines[9]);
        assertEquals("Second moment      <null>     10 <null> ", resultLines[10]);
        assertEquals("Sum of squares     <null>     55 <null> ", resultLines[11]);
    }

    public void testSimpleRun() throws Exception {
        numberAnalyzer.run(new MockInputRow().put(col1, 123.4f).put(col2, 1234L).put(col3, (byte) 12), 1);
        numberAnalyzer.run(new MockInputRow().put(col1, 567.8f).put(col2, 5678L).put(col3, (byte) 34), 1);

        final CrosstabResult result = numberAnalyzer.getResult();
        final String[] resultLines = new CrosstabTextRenderer().render(result).split("\n");
        assertEquals(12, resultLines.length);
        assertEquals("                      foo    bar   w00p ", resultLines[0]);
        assertEquals("Row count               2      2      2 ", resultLines[1]);
        assertEquals("Null count              0      0      0 ", resultLines[2]);
        assertEquals("Highest value       567.8   5678     34 ", resultLines[3]);
        assertEquals("Lowest value        123.4   1234     12 ", resultLines[4]);
        assertEquals("Sum                 691.2   6912     46 ", resultLines[5]);
        assertEquals("Mean                345.6   3456     23 ", resultLines[6]);
        assertEquals("Geometric mean      264.7 2647.01   20.2 ", resultLines[7]);
        assertEquals("Standard deviation 314.24 3142.38  15.56 ", resultLines[8]);
        assertEquals("Variance           98745.67 9874568    242 ", resultLines[9]);
        assertEquals("Second moment      98745.67 9874568    242 ", resultLines[10]);
        assertEquals("Sum of squares     337624.39 33762440   1300 ", resultLines[11]);
    }

}
