package solver.tests;

import static solver.tests.utility.NonogramPageUtility.*;

public class NonogramSolver {
    public static void main(String[] args) {
        init();
        openNonogramAndDefineTableSize(73850);
        fillInCellsInColumn();
        fillInCellsInRow();
        quit();
    }
}
