package solver.tests.utility;

import com.microsoft.playwright.*;
import solver.tests.constants.CellStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static solver.tests.constants.CellStatus.*;

public class NonogramPageUtility {
    static Page page;
    static Browser browser;
    static Playwright playwright;
    static BrowserContext context;
    private static int leftLength;
    private static int leftWidth;
    private static int rightLength;
    private final static String BASE_URI = "https://www.nonograms.org/nonograms/i/";
    private final static String RIGHT_WIDTH_XPATH = "//td[@class='nmtt']//tr";
    private final static String RIGHT_LENGTH_XPATH = "//td[@class='nmtt']//tr[1]//td";
    private final static String LEFT_WIDTH_XPATH = "//td[@class='nmtl']//tr";
    private final static String LEFT_LENGTH_XPATH = "//td[@class='nmtl']//tr[1]//td";
    private final static String ALL_RIGHT_CELLS_XPATH = "//td[@class='nmtt']//tr//td";
    private final static String ALL_LEFT_CELLS_XPATH = "//td[@class='nmtl']//tr//td";
    private final static String ALL_INNER_CELLS_XPATH = "//td[@class='nmtc']//tr//td";
    private final static String CELL_TO_CLICK_XPATH = "//td[@class='nmtc']//tr[%s]//td[%s]";

    public static void init() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        context = browser.newContext();
        page = context.newPage();
    }

    public static void quit() {
        context.close();
        playwright.close();
    }

    public static void openNonogram(int nonogramNumber) {
        page.navigate(BASE_URI + nonogramNumber);
        rightLength = page.locator(RIGHT_LENGTH_XPATH).count();
        leftLength = page.locator(LEFT_LENGTH_XPATH).count();
        leftWidth = page.locator(LEFT_WIDTH_XPATH).count();
    }

    public static List<List<String>> getRightPanelColumnList() {
        var values = page.locator(ALL_RIGHT_CELLS_XPATH).allInnerTexts();
        return IntStream.range(0, rightLength)
                .mapToObj(i -> IntStream
                        .iterate(i, j -> j < values.size(), j -> j + rightLength)
                        .mapToObj(values::get)
                        .map(String::trim)
                        .toList())
                .toList();
    }

    public static List<List<String>> getLeftPanelRowList() {
        var values = page.locator(ALL_LEFT_CELLS_XPATH).allInnerTexts();
        return IntStream.range(0, values.size())
                .filter(i -> i % leftLength == 0)
                .mapToObj(i -> values.subList(i, Math.min(i + leftLength, values.size())))
                .toList();
    }

    public static List<CellStatus> getAllCellsStatuses() {
        List<CellStatus> statuses = new ArrayList<>();
        var allInnerCells = page.locator(ALL_INNER_CELLS_XPATH).all();
        for (Locator locator : allInnerCells) {
            var style = locator.getAttribute("style");
            if (style == null) {
                statuses.add(WHITE);
            } else if (style.contains("background-image")) {
                statuses.add(CROSS);
            } else if (style.contains("background-color")) {
                statuses.add(BLACK);
            } else {
                statuses.add(WHITE);
            }
        }
        return statuses;
    }

    public static List<CellStatus> getColumnStatuses(int startIndex) {
        var allStatuses = getAllCellsStatuses();
        return IntStream
                .iterate(startIndex, i -> i < allStatuses.size(), i -> i + rightLength)
                .mapToObj(allStatuses::get)
                .toList();
    }

    public static List<CellStatus> getRowStatuses(int row) {
        var allStatuses = getAllCellsStatuses();
        var s = IntStream.range(0, allStatuses.size())
                .filter(i -> i % rightLength == 0)
                .mapToObj(i -> allStatuses.subList(i, Math.min(i + rightLength, allStatuses.size())))
                .toList();
        return s.get(row);
    }

    public static void fillInCellsInColumn() {
        var columnList = getRightPanelColumnList();
        for (int column = 0; column < columnList.size(); column++) {
            for (Integer cellToClick : getCellsToClickList(columnList.get(column), leftWidth)) {
                page.locator(String.format(CELL_TO_CLICK_XPATH, cellToClick, column + 1)).click();
            }
        }
    }

    private static List<Integer> mapToIntList(List<String> values) {
        return values
                .stream()
                .filter(value -> value.matches("-?\\d+(\\.\\d+)?"))
                .map(Integer::parseInt)
                .toList();
    }

    private static List<Integer> getCellsToClickList(List<String> line, int lineSize) {
        var cellsToClick = new ArrayList<Integer>();
        var integers = mapToIntList(line);
        var difference = lineSize - (integers
                .stream()
                .mapToInt(i -> i)
                .sum() + (integers.size() - 1));
        var differences = integers
                .stream()
                .map(i -> i - difference)
                .toList();
        if (differences.stream().anyMatch(i -> i > 0)) {
            int counter = 0;
            for (int i = 0; i < integers.size(); i++) {
                if (differences.get(i) > 0) {
                    int differenceDowngrade = differences.get(i);
                    while (differenceDowngrade != 0) {
                        cellsToClick.add(counter + integers.get(i) - differenceDowngrade + 1);
                        differenceDowngrade--;
                    }
                    counter += integers.get(i) + 1;
                } else {
                    counter += integers.get(i) + 1;
                }
            }
        }
        return cellsToClick;
    }
}