package implementation;


import declaration.stock.Stock;
import declaration.stock.factory.StockFactory;
import declaration.stock.factory.StockFactoryProvider;
import declaration.streamsupport.StreamUtil;
import declaration.CoreUtil;
import declaration.cmp.Compare;
import declaration.condition.*;
import declaration.database.Database;
import declaration.io.CsvReader;
import declaration.query.extension.Ext;
import declaration.query.extension.NormalFormatOrder;
import declaration.query.extension.Range;
import declaration.query.extension.Sort;
import declaration.stock.Field;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Time;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static declaration.stock.Field.STOCK_CODE;
import static declaration.stock.Field.TRADE_DATE;
import static implementation.Main.DailyTradeImpl.nullMinComparator;

public class Main implements CoreUtil {

    static class StockImpl implements Stock {

        String stockCode;
        String stockName;
        LocalDate listDate;
        String companyNameCn;
        String companyNameEn;
        String industryCode1;
        String industryName3;
        String provinceName;
        String cityName;
        Double initialPublicOfferPrice;
        String initialPublicOfferCurrency;
        Double shareInitialPublicOfferNumbers;
        String marketType;

        public static Comparator<StockImpl> fromField(Field field) {
            switch (field) {
                case STOCK_CODE -> {
                    return Comparator.comparing(o -> o.stockCode);
                }
                case LIST_DATE -> {
                    return nullMinComparator(o -> o.listDate);
                }
                case STOCK_NAME -> {
                    return nullMinComparator(o -> o.stockName);
                }
                case COMPANY_NAME -> {
                    return nullMinComparator(o -> o.companyNameCn);
                }
                case COMPANY_EN_NAME -> {
                    return nullMinComparator(o -> o.companyNameEn);
                }
                case INDUSTRY_CODE_A -> {
                    return nullMinComparator(o -> o.industryCode1);
                }
                case INDUSTRY_NAME_C -> {
                    return nullMinComparator(o -> o.industryName3);
                }
                case PROVINCE -> {
                    return nullMinComparator(o -> o.provinceName);
                }
                case CITY -> {
                    return nullMinComparator(o -> o.cityName);
                }
                case IPO_PRICE -> {
                    return nullMinComparator(o -> o.initialPublicOfferPrice);
                }
                case IPO_CURRENCY -> {
                    return nullMinComparator(o -> o.initialPublicOfferCurrency);
                }
                case IPO_SHARES -> {
                    return nullMinComparator(o -> o.shareInitialPublicOfferNumbers);
                }
                case MARKET_TYPE -> {
                    return nullMinComparator(o -> o.marketType);
                }
                default -> {
                    throw new RuntimeException("No such field in stock to sort. ");
                }
            }
        }

        // assert the condition is only related to this stock info
        public boolean handleCondition(Condition condition) {
            if (condition instanceof AndCondition c) {
                var cs = c.conditions();
                return cs.stream().allMatch(this::handleCondition);
            }
            if (condition instanceof OrCondition c) {
                var cs = c.conditions();
                return cs.stream().anyMatch(this::handleCondition);
            }
            if (condition instanceof NotCondition c) {
                var cs = c.condition();
                return !handleCondition(cs);
            }
            if (condition instanceof CodeCondition c) {
                switch (c.field()) {
                    case STOCK_CODE -> {
                        var cc = c.code();
                        return cc.equals(this.stockCode);
                    }
                    case INDUSTRY_CODE_A -> {
                        var cc = c.code();
                        return cc.equals(this.industryCode1);
                    }
                    case MARKET_TYPE -> {
                        var cc = c.code();
                        return cc.equals(this.marketType);
                    }
                    default -> {
                        throw new RuntimeException("No such field in stock info");
                    }
                }
            }
            if (condition instanceof PatternCondition c) {
                switch (c.field()) {
                    case STOCK_CODE -> {
                        var p = c.pattern();
                        return p.matcher(this.stockCode).matches();
                    }
                    case INDUSTRY_CODE_A -> {
                        var p = c.pattern();
                        return this.industryCode1 != null && p.matcher(this.industryCode1).matches();
                    }
                    case MARKET_TYPE -> {
                        var p = c.pattern();
                        return this.marketType != null && p.matcher(this.marketType).matches();
                    }
                    case STOCK_NAME -> {
                        var p = c.pattern();
                        return this.stockName != null && p.matcher(this.stockName).matches();
                    }
                    case COMPANY_NAME -> {
                        var p = c.pattern();
                        return this.companyNameCn != null && p.matcher(this.companyNameCn).matches();
                    }
                    case COMPANY_EN_NAME -> {
                        var p = c.pattern();
                        return this.companyNameEn != null && p.matcher(this.companyNameEn).matches();
                    }
                    case INDUSTRY_NAME_C -> {
                        var p = c.pattern();
                        return this.industryName3 != null && p.matcher(this.industryName3).matches();
                    }
                    case PROVINCE -> {
                        var p = c.pattern();
                        return this.provinceName != null && p.matcher(this.provinceName).matches();
                    }
                    case CITY -> {
                        var p = c.pattern();
                        return this.cityName != null && p.matcher(this.cityName).matches();
                    }
                    case IPO_CURRENCY -> {
                        var p = c.pattern();
                        return this.initialPublicOfferCurrency != null && p.matcher(this.initialPublicOfferCurrency).matches();
                    }
                    default -> {
                        throw new RuntimeException("No such field in stock info");
                    }
                }
            }
            if (condition instanceof DateCondition c) {
                if (Objects.requireNonNull(c.dateField()) == Field.LIST_DATE) {
                    return this.listDate != null && c.order().test(this.listDate, c.time());
                }
                throw new RuntimeException("No such field in stock info");
            }
            if (condition instanceof ValueCondition c) {
                var f = c.field();
                switch (f) {
                    case IPO_PRICE -> {
                        return this.initialPublicOfferPrice != null && c.compare().test(this.initialPublicOfferPrice, c.value());
                    }
                    case IPO_SHARES -> {
                        return this.shareInitialPublicOfferNumbers != null && c.compare().test(this.shareInitialPublicOfferNumbers, c.value());
                    }
                    default -> {
                        throw new RuntimeException("No such field in stock info");
                    }
                }
            }
            throw new RuntimeException("No such field in stock info");
        }

        static class Factory implements StockFactory {

            public StreamUtil streamUtil;

            public List<Optional<Integer>> fieldIdx;
            public int size;

            public Factory(String tableHeader, StreamUtil streamUtil) throws StockFactoryProvider.StockFactoryConstructorException {
                this.streamUtil = Objects.requireNonNull(streamUtil);
                var exception = new StockFactoryProvider.StockFactoryConstructorException();
                // check 'data' is valid (single line)
                {
                    if (tableHeader.contains(System.lineSeparator())) {
                        exception.singleLine = false;
                        throw exception;
                    }
                }
                // check 'data' is valid (csv format)
                List<String> l;
                {
                    try {
                        l = splitAsList(tableHeader);
                    } catch (RuntimeException e) {
                        exception.quotationMarks = false;
                        throw exception;
                    }
                }
                Stream<Optional<Integer>> fieldIdx = fieldNames.stream().map(n -> {
                    var ls = l.stream();
                    var is = IntStream.iterate(0, i -> i + 1).boxed();
                    Stream<Optional<Integer>> zip = streamUtil.zip(ls, is, (s, idx) -> {
                        if (n.equals(s)) {
                            return Optional.of(idx);
                        } else {
                            return Optional.empty();
                        }
                    });
                    var zip2 = streamUtil.filterMap(zip, Function.identity());
                    try {
                        var r = streamUtil.onlyOne(zip2);
                        return Optional.of(r);
                    } catch (StreamUtil.NoElementInStreamException|StreamUtil.MoreThanOneElementInStreamException e) {
                        return Optional.empty();
                    }
                });
                var idx = fieldIdx.toList();
                if (idx.get(0).isEmpty()) {
                    exception.containStockCode = false;
                    throw exception;
                }
                this.fieldIdx = idx;
                this.size = l.size();
            }

            public final static List<String> fieldNames = List.of(
                    "Stkcd",
                    "Stknme",
                    "Listdt",
                    "Conme",
                    "Conme_en",
                    "Indcd",
                    "Nnindcnme",
                    "PROVINCE",
                    "CITY",
                    "Ipoprc",
                    "Ipocur",
                    "Nshripo",
                    "Markettype"
            );

            @Override
            public Stock newStock(String data) throws StockInstantiationException {
                var exception = new StockInstantiationException();
                // check 'data' is valid (single line)
                {
                    if (data.contains(System.lineSeparator())) {
                        exception.singleLine = false;
                        throw exception;
                    }
                }
                // check 'data' is valid (csv format)
                List<String> l;
                try {
                    l = splitAsList(data);
                } catch (RuntimeException e) {
                    exception.quotationMarks = false;
                    throw exception;
                }
                // check 'data' column count is proper
                {
                    var s = l.size();
                    if (s != this.size) {
                        exception.unMatchColumnCount = List.of(this.size, s);
                        throw exception;
                    }
                }
                var length = this.fieldIdx.size();
                var stock = new StockImpl();
                for (int i = 0; i<length; ++i) {
                    var idx = this.fieldIdx.get(i);
                    if (idx.isEmpty()) {
                        continue;
                    }
                    int iGet = idx.get();
                    var actualValue = l.get(iGet);
                    try {
                        switch (i) {
                            case 0 -> stock.stockCode = actualValue;
                            case 1 -> stock.stockName = actualValue;
                            case 2 ->
                                    stock.listDate = LocalDate.parse(actualValue, DateTimeFormatter.ofPattern("yyyy/M/d"));
                            case 3 -> stock.companyNameCn = actualValue;
                            case 4 -> stock.companyNameEn = actualValue;
                            case 5 -> stock.industryCode1 = actualValue;
                            case 6 -> stock.industryName3 = actualValue;
                            case 7 -> stock.provinceName = actualValue;
                            case 8 -> stock.cityName = actualValue;
                            case 9 -> stock.initialPublicOfferPrice = Double.parseDouble(actualValue);
                            case 10 -> stock.initialPublicOfferCurrency = actualValue;
                            case 11 -> stock.shareInitialPublicOfferNumbers = Double.parseDouble(actualValue);
                            case 12 -> stock.marketType = actualValue;
                            default -> throw new RuntimeException();
                        }
                    } catch (RuntimeException ignored) {
                        if (!(exception.fieldParseFailed instanceof HashSet<Field>)) {
                            exception.fieldParseFailed = new HashSet<>(exception.fieldParseFailed);
                        }
                        var fieldName = fieldNames.get(i);
                        Arrays.stream(Field.values()).filter(f -> f.name().equals(fieldName)).findAny().ifPresent(exception.fieldParseFailed::add);
                    }
                }
                if (!exception.fieldParseFailed.isEmpty()) {
                    throw exception;
                }
                return stock;
            }
        }

        public final static DateTimeFormatter localDateTimeDefaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        @Override
        public String getField(Field field) throws NoSuchFieldException, NoSuchDataException {
            var f = getRawField(field);
            switch (field) {
                case STOCK_CODE, INDUSTRY_CODE_A, IPO_CURRENCY, MARKET_TYPE -> {
                    return (String) f;
                }
                case LIST_DATE -> {
                    return ((LocalDate) f).format(localDateTimeDefaultFormatter);
                }
                case STOCK_NAME, COMPANY_NAME, COMPANY_EN_NAME, INDUSTRY_NAME_C, PROVINCE, CITY -> {
                    return (String) f;
                }
                case IPO_PRICE, IPO_SHARES -> {
                    var formatter = new Formatter();
                    var p = formatter.format("%.2f", (Double) f);
                    var r = p.toString();
                    return r;
                }
                default -> {
                    // impossible
                    throw new RuntimeException();
                }
            }
        }


        @Override
        public Object getRawField(Field field) throws NoSuchDataException, NoSuchFieldException {
            try {
                return switch (field) {
                    case STOCK_CODE -> Objects.requireNonNull(stockCode);
                    case STOCK_NAME -> Objects.requireNonNull(this.stockName);
                    case LIST_DATE -> Objects.requireNonNull(this.listDate);
                    case COMPANY_NAME -> Objects.requireNonNull(this.companyNameCn);
                    case COMPANY_EN_NAME -> Objects.requireNonNull(this.companyNameEn);
                    case INDUSTRY_CODE_A -> Objects.requireNonNull(this.industryCode1);
                    case INDUSTRY_NAME_C -> Objects.requireNonNull(this.industryName3);
                    case PROVINCE -> Objects.requireNonNull(this.provinceName);
                    case CITY -> Objects.requireNonNull(this.cityName);
                    case IPO_PRICE -> Objects.requireNonNull(this.initialPublicOfferPrice);
                    case IPO_CURRENCY -> Objects.requireNonNull(this.initialPublicOfferCurrency);
                    case IPO_SHARES -> Objects.requireNonNull(this.shareInitialPublicOfferNumbers);
                    case MARKET_TYPE -> Objects.requireNonNull(this.marketType);
                    default -> throw new NoSuchFieldException();
                };
            } catch (NullPointerException ignored) {
                throw new NoSuchDataException();
            }
        }

    }

    static class DailyTradeImpl implements Stock {

        String stockCode;
        LocalDate tradeDate;
        Double openPrice;
        Double highestPrice;
        Double lowestPrice;
        Double closePrice;
        Double dividend;

        // Done
        public boolean handleCondition(Condition condition) {
            // TODO
            if (condition instanceof AndCondition c) {
                var cs = c.conditions();
                return cs.stream().allMatch(this::handleCondition);
            }
            if (condition instanceof OrCondition c) {
                var cs = c.conditions();
                return cs.stream().anyMatch(this::handleCondition);
            }
            if (condition instanceof NotCondition c) {
                var cs = c.condition();
                return !handleCondition(cs);
            }
            if (condition instanceof CodeCondition c) {
                switch (c.field()) {
                    case STOCK_CODE -> {
                        var cc = c.code();
                        return cc.equals(this.stockCode);
                    }
                    default -> {
                        throw new RuntimeException("No such field in daily trade info");
                    }
                }
            }
            if (condition instanceof PatternCondition c) {
                switch (c.field()) {
                    case STOCK_CODE -> {
                        var p = c.pattern();
                        return p.matcher(this.stockCode).matches();
                    }
                    default -> {
                        throw new RuntimeException("No such field in daily trade info");
                    }
                }
            }
            if (condition instanceof DateCondition c) {
                if (Objects.requireNonNull(c.dateField()) == Field.TRADE_DATE) {
                    return this.tradeDate != null && c.order().test(this.tradeDate, c.time());
                }
                throw new RuntimeException("No such field in daily trade info");
            }
            if (condition instanceof ValueCondition c) {
                var f = c.field();
                switch (f) {
                    case OPEN_PRICE -> {
                        return this.openPrice != null && c.compare().test(this.openPrice, c.value());
                    }
                    case HIGH_PRICE -> {
                        return this.highestPrice != null && c.compare().test(this.highestPrice, c.value());
                    }
                    case LOW_PRICE -> {
                        return this.lowestPrice != null && c.compare().test(this.lowestPrice, c.value());
                    }
                    case CLOSE_PRICE -> {
                        return this.closePrice != null && c.compare().test(this.closePrice, c.value());
                    }
                    case DIVIDEND_CLOSE_PRICE -> {
                        return this.dividend != null && c.compare().test(this.dividend, c.value());
                    }
                    default -> {
                        throw new RuntimeException("No such field in daily trade info");
                    }
                }
            }
            throw new RuntimeException("No such field in daily trade info");
        }

        public static <T, R extends Comparable<R>> Comparator<T> nullMinComparator(Function<T, R> map) {
            return (t1, t2) -> {
                var r1 = map.apply(t1);
                var r2 = map.apply(t2);
                if (r1 == null) {
                    return r2 == null ? 0 : -1;
                }
                if (r2 == null) {
                    return 1;
                }
                return r1.compareTo(r2);
            };
        }


        // Done
        public static Comparator<DailyTradeImpl> fromField(Field field) {
            // TODO
            switch (field) {
                case STOCK_CODE -> {
                    return Comparator.comparing(o -> o.stockCode);
                }
                case TRADE_DATE -> {
                    return nullMinComparator(o -> o.tradeDate);
                }
                case OPEN_PRICE -> {
                    return nullMinComparator(o -> o.openPrice);
                }
                case HIGH_PRICE -> {
                    return nullMinComparator(o -> o.highestPrice);
                }
                case LOW_PRICE -> {
                    return nullMinComparator(o -> o.lowestPrice);
                }
                case CLOSE_PRICE -> {
                    return nullMinComparator(o -> o.closePrice);
                }
                case DIVIDEND_CLOSE_PRICE -> {
                    return nullMinComparator(o -> o.dividend);
                }
                default -> {
                    throw new RuntimeException("No such field in stock to sort. ");
                }
            }
        }

        public final static DateTimeFormatter localDateTimeDefaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Done
        @Override
        public String getField(Field field) throws NoSuchDataException, NoSuchFieldException {
            // TODO
            var f = getRawField(field);
            switch (field) {
                case STOCK_CODE -> {
                    return (String) f;
                }
                case TRADE_DATE -> {
                    return ((LocalDate) f).format(localDateTimeDefaultFormatter);
                }
                case OPEN_PRICE, HIGH_PRICE, LOW_PRICE, CLOSE_PRICE, DIVIDEND_CLOSE_PRICE -> {
                    var formatter = new Formatter();
                    var p = formatter.format("%.2f", (Double) f);
                    return p.toString();
                }
                default -> {
                    // impossible
                    throw new RuntimeException();
                }
            }

        }

        // Done
        @Override
        public Object getRawField(Field field) throws NoSuchDataException, NoSuchFieldException {
            // TODO
            try {
                return switch (field) {
                    case STOCK_CODE -> Objects.requireNonNull(this.stockCode); //change
                    case TRADE_DATE -> Objects.requireNonNull(this.tradeDate);
                    case OPEN_PRICE -> Objects.requireNonNull(this.openPrice);
                    case HIGH_PRICE -> Objects.requireNonNull(this.highestPrice);
                    case LOW_PRICE -> Objects.requireNonNull(this.lowestPrice);
                    case CLOSE_PRICE -> Objects.requireNonNull(this.closePrice);
                    case DIVIDEND_CLOSE_PRICE -> Objects.requireNonNull(this.dividend);
                    default -> throw new NoSuchFieldException();
                };
            } catch (NullPointerException ignored) {
                throw new NoSuchDataException();
            }
        }

        public static class Factory implements StockFactory {

            public static final List<String> fieldNames = List.of(
                    "Stkcd",
                    "Trddt",
                    "Opnprc",
                    "Hiprc",
                    "Loprc",
                    "Clsprc",
                    "Adjprcnd"
            );
            public List<Optional<Integer>> fieldIdx;
            public StreamUtil streamUtil;
            public int size;

            public Factory(String tableHeader, StreamUtil streamUtil) throws StockFactoryProvider.StockFactoryConstructorException {
                // TODO
                this.streamUtil = Objects.requireNonNull(streamUtil);
                var exception = new StockFactoryProvider.StockFactoryConstructorException();
                // check 'data' is valid (single line)
                {
                    if (tableHeader.contains(System.lineSeparator())) {
                        exception.singleLine = false;
                        throw exception;
                    }
                }
                // check 'data' is valid (csv format)
                List<String> l;
                {
                    try {
                        l = splitAsList(tableHeader);
                    } catch (RuntimeException e) {
                        exception.quotationMarks = false;
                        throw exception;
                    }
                }
                Stream<Optional<Integer>> fieldIdx = fieldNames.stream().map(n -> {
                    var ls = l.stream();
                    var is = IntStream.iterate(0, i -> i + 1).boxed();
                    Stream<Optional<Integer>> zip = streamUtil.zip(ls, is, (s, idx) -> {
                        if (n.equals(s)) {
                            return Optional.of(idx);
                        } else {
                            return Optional.empty();
                        }
                    });
                    var zip2 = streamUtil.filterMap(zip, Function.identity());
                    try {
                        var r = streamUtil.onlyOne(zip2);
                        return Optional.of(r);
                    } catch (StreamUtil.NoElementInStreamException|StreamUtil.MoreThanOneElementInStreamException e) {
                        return Optional.empty();
                    }
                });
                var idx = fieldIdx.toList();
                if (idx.get(0).isEmpty()) {
                    exception.containStockCode = false;
                    throw exception;
                }
                this.fieldIdx = idx;
                this.size = l.size();
            }


            @Override
            public Stock newStock(String data) throws StockInstantiationException {
                // TODO
                var exception = new StockInstantiationException();
                // check 'data' is valid (single line)
                {
                    if (data.contains(System.lineSeparator())) {
                        exception.singleLine = false;
                        throw exception;
                    }
                }
                // check 'data' is valid (csv format)
                List<String> l;
                try {
                    l = splitAsList(data);
                } catch (RuntimeException e) {
                    exception.quotationMarks = false;
                    throw exception;
                }
                // check 'data' column count is proper
                {
                    var s = l.size();
                    if (s != this.size) {
                        exception.unMatchColumnCount = List.of(this.size, s);
                        throw exception;
                    }
                }
                var length = this.fieldIdx.size();
                var stock = new DailyTradeImpl();
                for (int i = 0; i<length; ++i) {
                    var idx = this.fieldIdx.get(i);
                    if (idx.isEmpty()) {
                        continue;
                    }
                    int iGet = idx.get();
                    var actualValue = l.get(iGet);
                    try {
                        switch (i) {
                            case 0 -> stock.stockCode = actualValue;
                            case 1 ->
                                    stock.tradeDate = LocalDate.parse(actualValue, DateTimeFormatter.ofPattern("yyyy/M/d"));
                            case 2 -> stock.openPrice = Double.parseDouble(actualValue);
                            case 3 -> stock.highestPrice = Double.parseDouble(actualValue);
                            case 4 -> stock.lowestPrice = Double.parseDouble(actualValue);
                            case 5 -> stock.closePrice = Double.parseDouble(actualValue);
                            case 6 -> stock.dividend = Double.parseDouble(actualValue);
                            default -> throw new RuntimeException();
                        }
                    } catch (RuntimeException ignored) {
                        if (!(exception.fieldParseFailed instanceof HashSet<Field>)) {
                            exception.fieldParseFailed = new HashSet<>(exception.fieldParseFailed);
                        }
                        var fieldName = fieldNames.get(i);
                        //change, only care about stock code and trade date!
                        // IMPORTANT!
                        Field[] fields = {STOCK_CODE, TRADE_DATE};
                        Arrays.stream(fields).filter(f -> f.fieldName.equals(fieldName)).findAny().ifPresent(exception.fieldParseFailed::add);
                    }
                }
                if (!exception.fieldParseFailed.isEmpty()) {
                    throw exception;
                }
                return stock;
            }

        }
    }


    private static List<String> splitAsList(String origin) {
        var rst = new ArrayList<String>();
        var cps = origin.codePoints().toArray();
        var idx = 0;
        var sb = new StringBuilder();
        var isQuote = false;
        while (idx<cps.length) {
            var c = cps[idx];
            switch (c) {
                case '"':
                    if (!isQuote) {
                        if (!sb.isEmpty()) {
                            throw new RuntimeException("Unexpected quote. ");
                        }
                        isQuote = true;
                    } else {
                        if (idx + 1<cps.length && cps[idx + 1] == '"') {
                            sb.appendCodePoint(c);
                            idx += 1;
                        } else if (idx + 1 == cps.length || cps[idx + 1] == ',') {
                            isQuote = false;
                        }
                    }
                    break;
                case ',':
                    if (isQuote) {
                        sb.appendCodePoint(c);
                    } else {
                        rst.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    break;
                default:
                    sb.appendCodePoint(c);
                    break;
            }
            idx += 1;
        }
        if (isQuote) {
            throw new RuntimeException("Unmatched quote. ");
        }
        rst.add(sb.toString());
        return rst;
    }

    @Override
    public CsvReader getCsvReader() {
        // TODO
        return (Path path) -> {
            try {
                FileInputStream fileInputStream = new FileInputStream(path.toString());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                byte[] buffer = new byte[4];
                int bytesRead = bufferedInputStream.read(buffer);

                Charset charset = null;
                if (bytesRead>=3 && buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB && buffer[2] == (byte) 0xBF) {
                    charset = StandardCharsets.UTF_8;// utf-8 BOM
                } else if (bytesRead>=3 && buffer[0] == (byte) 0xE5 && buffer[1] == (byte) 0x9B && buffer[2] == (byte) 0x9E) {
                    charset = StandardCharsets.UTF_8;//E59B9E UTF-8
                } else if (bytesRead>=4 && buffer[0] == 0x00 && buffer[1] == 0x00 && buffer[2] == (byte) 0xFE && buffer[3] == (byte) 0xFF) {
                    charset = Charset.forName("UTF-32");
                } else if (bytesRead>=4 && buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE && buffer[2] == 0x00 && buffer[3] == 0x00) {
                    charset = Charset.forName("UTF-32");
                } else if (bytesRead>=2 && (buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF || buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE)) {
                    charset = StandardCharsets.UTF_16;
                } else if (bytesRead>=4 && buffer[0] == (byte) 34 && buffer[1] == (byte) 83 && buffer[2] == (byte) 116 && buffer[3] == (byte) 107) {
                    charset = Charset.forName("GB18030");
                } else {
                    charset = StandardCharsets.UTF_8;
                }

                List<String> lines = Files.readAllLines(path, charset);
                //the first string in localtest5, company2.csv is wrong coded.
                //handle the first line
                if (lines.get(0).startsWith("\uFEFF")) { //IMPORTANT
                    String s = lines.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i<s.length(); i++) {
                        sb.append(s.charAt(i));
                    }
                    lines.set(0, sb.toString());
                }
                return lines;
            } catch (IOException e) {
                throw new FileNotFoundException(path.toString());
            }
        };
    }


    @Override
    public StockFactoryProvider getAbstractStockFactory() {
        // TODO
        return new StockFactoryProvider() {
            @Override
            public StockFactory newStockFactory(String tableHeader) throws StockFactoryConstructorException {
                //Stknme,Listdt,Curtrd,Ipoprm,Ipoprc,Stkcd,"""Stkcd",Listdt,Conme,Indcd
                String stk = "Stkcd";
                int stkCnt = occurrence(tableHeader, "Stkcd");
                int trdCnt = occurrence(tableHeader, "Trddt");
                if (stkCnt == 1 && trdCnt == 1) {
                    return new DailyTradeImpl.Factory(tableHeader, getStreamUtil());
                } else if (stkCnt == 1 && trdCnt == 0) {
                    return new StockImpl.Factory(tableHeader, getStreamUtil());
                } else {
                    throw new StockFactoryProvider.StockFactoryConstructorException();
                }
            }

            private int occurrence(String tableHeader, String target) throws StockFactoryConstructorException {
                String[] arr = tableHeader.split(",");
                for (int i = 0; i<arr.length; i++) {
                    String s = arr[i];
                    while (true) {
                        if (s.startsWith("\"") && s.endsWith("\"")) {
                            if (s.length()>=2) {
                                s = s.substring(1, s.length() - 1); // remove quotation marks at the beginning and end of the string
                            } else {
                                s = "";
                                break;
                            }
                        } else break;
                    }
                    arr[i] = s;
                }
                int cnt = 0;
                for (String s : arr) {
                    if (s.equals(target)) cnt++;
                }
                return cnt;
            }
        };
    }

    @Override
    public Database getDatabase() {
        return new Database() {
            private boolean state = false;

            private ArrayList<StockImpl> stocks = new ArrayList<>();
            private ArrayList<DailyTradeImpl> trades = new ArrayList<>();

            private Set<String> stockCodes = new HashSet<>();

            record DailyTradeKey(String stockCode, LocalDate date) {
            }

            private Set<DailyTradeKey> dailyTradeKeys = new HashSet<>();

            @Override
            public InsertResult insert(List<Stock> stocks) throws InvalidInsertStateException {
                if (state) throw new InvalidInsertStateException();
                long cnt = 0;
                List<Stock> failed = new ArrayList<>();
                for (Stock stock : stocks) {
                    if (stock instanceof StockImpl si) {
                        var key = si.stockCode;
                        if (stockCodes.contains(key)) {
                            failed.add(stock);
                        } else {
                            this.stocks.add(si);
                            cnt += 1;
                        }
                    } else if (stock instanceof DailyTradeImpl dt) {
                        var key = new DailyTradeKey(dt.stockCode, dt.tradeDate);
                        if (dailyTradeKeys.contains(key)) {
                            failed.add(stock);
                        } else {
                            dailyTradeKeys.add(key);
                            this.trades.add(dt);
                            cnt += 1;
                        }
                    } else {
                        failed.add(stock);
                    }
                }
                return new InsertResult(cnt, failed);
            }

            @Override
            public Supplier<List<?>> query(Condition condition, Ext... extensions) throws QueryException {
                state = true;
                var conditionFields = getRelatedFields(condition);
                var exts = Arrays.stream(extensions);
                var su = getStreamUtil();
                var p = su.split(exts, (e) -> {
                    if (e instanceof Sort s) {
                        return Optional.of(s);
                    } else {
                        return Optional.empty();
                    }
                });
                var sorts = p.second();
                var p2 = su.split(p.first(), (e) -> {
                    if (e instanceof NormalFormatOrder o) {
                        return Optional.of(o);
                    } else {
                        return Optional.empty();
                    }
                });
                var orders = p2.second();
                var p3 = su.split(p2.first(), (e) -> {
                    if (e instanceof Range l) {
                        return Optional.of(l);
                    } else {
                        return Optional.empty();
                    }
                });
                var ranges = p3.second();
                var nulls = p3.first();
                var a = nulls.findAny().isEmpty();
                if (!a) {
                    // impossible
                    throw new RuntimeException();
                }
                var exception = new QueryException();
                List<Field> orderFields = Collections.emptyList();
                NormalFormatOrder order = null;
                // check order exist or not
                {
                    var set = orders.collect(Collectors.toSet());
                    if (set.size()>1) {
                        exception.conflictOrders = set;
                        throw exception;
                    }
                    if (set.size() == 1) {
                        order = set.stream().findAny().get();
                        orderFields = order.fields();
                    }
                }
                // check range
                Range r;
                {
                    try {
                        r = su.onlyOne(ranges);
                    } catch (StreamUtil.MoreThanOneElementInStreamException e) {
                        exception.isRangeErr = true;
                        throw exception;
                    } catch (StreamUtil.NoElementInStreamException e) {
                        // do nothing
                        r = null;
                    }
                }
                // check sorts
                List<Sort> sortSorts;
                List<Field> sortFields;
                {
                    sortSorts = sorts.collect(Collectors.toList());
                    sortFields = sortSorts.stream().map(Sort::field).collect(Collectors.toList());
                }
                Set<Field> allFields = new HashSet<>();
                allFields.addAll(conditionFields);
                allFields.addAll(orderFields);
                allFields.addAll(sortFields);
                var errs = allFields.stream().filter(f -> {
                    switch (f) {
                        case STOCK_CODE, STOCK_NAME, LIST_DATE, COMPANY_NAME, COMPANY_EN_NAME, INDUSTRY_CODE_A, INDUSTRY_NAME_C, PROVINCE, CITY, IPO_PRICE, IPO_CURRENCY, IPO_SHARES, MARKET_TYPE -> {
                            return false;
                        }
                        case OPEN_PRICE, CLOSE_PRICE, HIGH_PRICE, LOW_PRICE, DIVIDEND_CLOSE_PRICE, TRADE_DATE -> {
                            return false;
                        }
                        default -> {
                            return true;
                        }
                    }
                }).collect(Collectors.toSet());
                if (!errs.isEmpty()) {
                    exception.noSuchFields = errs;
                    throw exception;
                }
                // check if related stock info
                var stockInfo = allFields.stream().anyMatch(f -> {
                    switch (f) {
                        case STOCK_NAME, LIST_DATE, COMPANY_NAME, COMPANY_EN_NAME, INDUSTRY_CODE_A, INDUSTRY_NAME_C, PROVINCE, CITY, IPO_PRICE, IPO_CURRENCY, IPO_SHARES, MARKET_TYPE -> {
                            return true;
                        }
                        default -> {
                            return false;
                        }
                    }
                });
                var tradeInfo = allFields.stream().anyMatch(f -> {
                    switch (f) {
                        case OPEN_PRICE, CLOSE_PRICE, HIGH_PRICE, LOW_PRICE, DIVIDEND_CLOSE_PRICE, TRADE_DATE -> {
                            return true;
                        }
                        default -> {
                            return false;
                        }
                    }
                });
                // check if conflict
                if (stockInfo && tradeInfo) {
                    exception.isConditionConflict = true;
                    throw exception;
                }
                if (tradeInfo) {
                    var rr = r;
                    var o = order;
                    return () -> {
                        // trade info
                        var tradePass = new ArrayList<DailyTradeImpl>(trades.stream().filter(t -> t.handleCondition(condition)).toList());
                        // sort ~
                        for (int size = sortSorts.size(); size>0; size--) {
                            var field = sortSorts.get(size - 1);
                            var comparator = DailyTradeImpl.fromField(field.field());
                            if (!field.isAscending()) {
                                comparator = comparator.reversed();
                            }
                            tradePass.sort(comparator);
                        }
                        // range
                        var subList = rr == null ? tradePass : tradePass.subList(rr.left(), rr.right());
                        // format
                        List<?> rst;
                        if (o != null) {
                            var m = subList.stream().<List<String>>mapMulti((d, rstCall) -> {
                                var os = o.fields().stream();
                                var p4 = su.split(os, f -> {
                                    try {
                                        return Optional.of(d.getField(f));
                                    } catch (Stock.NoSuchDataException e) {
                                        return Optional.empty();
                                    } catch (Stock.NoSuchFieldException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                if (p4.first().findAny().isPresent()) {
                                    return;
                                }
                                var l = p4.second().toList();
                                rstCall.accept(l);
                            });
                            rst = m.toList();
                        } else {
                            rst = subList;
                        }
                        return rst;
                    };
                } else {
                    // stock info
                    var rr = r;
                    var o = order;
                    return () -> {
                        var stockPass = new ArrayList<StockImpl>(stocks.stream().filter(t -> t.handleCondition(condition)).toList());
                        // sort ~
                        for (int size = sortSorts.size(); size>0; size--) {
                            var field = sortSorts.get(size - 1);
                            var comparator = StockImpl.fromField(field.field());
                            if (!field.isAscending()) {
                                comparator = comparator.reversed();
                            }
                            stockPass.sort(comparator);
                        }
                        // range
                        var subList = rr == null ? stockPass : stockPass.subList(rr.left(), rr.right());
                        // format
                        List<?> rst;
                        if (o != null) {
                            var m = subList.stream().<List<String>>mapMulti((d, rstCall) -> {
                                var os = o.fields().stream();
                                var p4 = su.split(os, f -> {
                                    try {
                                        return Optional.of(d.getField(f));
                                    } catch (Stock.NoSuchDataException e) {
                                        return Optional.empty();
                                    } catch (Stock.NoSuchFieldException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                if (p4.first().findAny().isPresent()) {
                                    return;
                                }
                                var l = p4.second().toList();
                                rstCall.accept(l);
                            });
                            rst = m.toList();
                        } else {
                            rst = subList;
                        }
                        return rst;
                    };
                }
            }
        };
    }

    public static List<Field> getRelatedFields(Condition condition) {
        if (condition instanceof AndCondition a) {
            return a.conditions().stream().map(Main::getRelatedFields).flatMap(List::stream).toList();
        } else if (condition instanceof OrCondition a) {
            return a.conditions().stream().map(Main::getRelatedFields).flatMap(List::stream).toList();
        } else if (condition instanceof NotCondition a) {
            return getRelatedFields(a.condition());
        } else if (condition instanceof ValueCondition a) {
            return List.of(a.field());
        } else if (condition instanceof DateCondition a) {
            return List.of(a.dateField());
        } else if (condition instanceof PatternCondition a) {
            return List.of(a.field());
        } else if (condition instanceof CodeCondition a) {
            return List.of(a.field());
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public AtomicConditionFactory getAtomicConditionFactory() {
        return new AtomicConditionFactory() {

            public static void assertField(Field field) throws Stock.NoSuchFieldException {
                switch (field) {
                    case STOCK_CODE, STOCK_NAME, LIST_DATE, COMPANY_NAME, COMPANY_EN_NAME, INDUSTRY_CODE_A, INDUSTRY_NAME_C, PROVINCE, CITY, IPO_PRICE, IPO_CURRENCY, IPO_SHARES, MARKET_TYPE -> {
                        return;
                    }
                    case OPEN_PRICE, CLOSE_PRICE, HIGH_PRICE, LOW_PRICE, DIVIDEND_CLOSE_PRICE, TRADE_DATE -> {
                        return;
                    }
                    default -> {
                        throw new Stock.NoSuchFieldException();
                    }
                }
            }

            @Override
            public ValueCondition newValueCondition(Field field, double value, Compare compare) throws UnmatchException, Stock.NoSuchFieldException {
                // TODO!
                assertField(field);
                if (Double.isNaN(value)) { // !
                    throw new UnmatchException();
                }
                return switch (field) {
                    case OPEN_PRICE, CLOSE_PRICE, HIGH_PRICE, LOW_PRICE, DIVIDEND_CLOSE_PRICE,
                            IPO_PRICE, IPO_SHARES -> new ValueCondition(field, value, compare);
                    default -> throw new UnmatchException();
                };
            }

            @Override
            public DateCondition newDateCondition(Field field, LocalDate time, boolean allowUnknown, Compare compare) throws UnmatchException, Stock.NoSuchFieldException {
                // TODO
                assertField(field);
                return switch (field) {
                    case LIST_DATE, TRADE_DATE -> new DateCondition(field, time, compare, allowUnknown);
                    default -> throw new UnmatchException();
                };
            }

            @Override
            public PatternCondition newPatternCondition(Field field, Pattern pattern) throws UnmatchException, Stock.NoSuchFieldException {
                // TODO
                assertField(field);
                return switch (field) {
                    case STOCK_CODE, STOCK_NAME, COMPANY_NAME, COMPANY_EN_NAME, INDUSTRY_CODE_A, INDUSTRY_NAME_C,
                            PROVINCE, CITY, IPO_CURRENCY, MARKET_TYPE -> new PatternCondition(field, pattern);
                    default -> throw new UnmatchException();
                };
            }

            @Override
            public CodeCondition newCodeCondition(Field field, String code) throws UnmatchException, Stock.NoSuchFieldException {
                assertField(field);
                return switch (field) {
                    case STOCK_CODE, INDUSTRY_CODE_A -> new CodeCondition(field, code);
                    default -> throw new UnmatchException();
                };
            }

        };
    }

    @Override
    public StreamUtil getStreamUtil() {
        // TODO
        return new StreamUtil() {
            @Override
            public <R, T, U> Stream<R> zip(Stream<T> stream1, Stream<U> stream2, BiFunction<? super T, ? super U, ? extends R> map) {
                // TODO
                Spliterator<T> spliterator1 = stream1.spliterator();
                Spliterator<U> spliterator2 = stream2.spliterator();

                Iterator<T> iterator1 = Spliterators.iterator(spliterator1);
                Iterator<U> iterator2 = Spliterators.iterator(spliterator2);

                return StreamSupport.stream(
                        new Spliterators.AbstractSpliterator<R>(
                                Math.min(spliterator1.estimateSize(), spliterator2.estimateSize()),
                                Spliterator.ORDERED) {
                            @Override
                            public boolean tryAdvance(Consumer<? super R> action) {
                                if (iterator1.hasNext() && iterator2.hasNext()) {
                                    T t = iterator1.next();
                                    U u = iterator2.next();
                                    R result = map.apply(t, u);
                                    action.accept(result);
                                    return true;
                                }
                                return false;
                            }
                        },
                        false
                );
            }

            public <R, T, U> Stream<R> flatZip(Stream<T> stream1, Stream<U> stream2, BiFunction<? super T, ? super U, Stream<? extends R>> flatMap) {
                // TODO
                Spliterator<T> spliterator1 = stream1.spliterator();
                Spliterator<U> spliterator2 = stream2.spliterator();

                Iterator<T> iterator1 = Spliterators.iterator(spliterator1);
                Iterator<U> iterator2 = Spliterators.iterator(spliterator2);

                return StreamSupport.stream(
                        new Spliterators.AbstractSpliterator<R>(
                                Math.min(spliterator1.estimateSize(), spliterator2.estimateSize()),
                                Spliterator.ORDERED) {
                            @Override
                            public boolean tryAdvance(Consumer<? super R> action) {
                                if (iterator1.hasNext() && iterator2.hasNext()) {
                                    T t = iterator1.next();
                                    U u = iterator2.next();
                                    Stream<? extends R> resultStream = flatMap.apply(t, u);
                                    resultStream.forEach(action);
                                    return true;
                                }
                                return false;
                            }
                        },
                        false
                );

            }

            @Override
            public <T> T onlyOne(Stream<T> stream) throws NoElementInStreamException, MoreThanOneElementInStreamException {
                var it = stream.iterator();
                if (!it.hasNext()) {
                    throw new NoElementInStreamException();
                }
                var rst = it.next();
                if (it.hasNext()) {
                    throw new MoreThanOneElementInStreamException();
                }
                return rst;
            }

            static class ChuckIterator<T, U> {

                Deque<T> left = new ArrayDeque<>();
                Deque<U> right = new ArrayDeque<>();
                Spliterator<T> iterator;
                Function<? super T, Optional<? extends U>> filterMap;

                class LeftIterator implements Spliterator<T> {

                    @Override
                    public boolean tryAdvance(Consumer<? super T> consumer) {
                        while (true) {
                            var f = left.pollFirst();
                            if (f != null) {
                                consumer.accept(f);
                                return true;
                            }
                            var b = iterator.tryAdvance((t2) -> {
                                filterMap.apply(t2).ifPresentOrElse(right::addLast, () -> left.addLast(t2));
                            });
                            if (!b) {
                                return false;
                            }
                        }
                    }

                    @Override
                    public Spliterator<T> trySplit() {
                        return null;
                    }

                    @Override
                    public long estimateSize() {
                        return iterator.estimateSize();
                    }

                    @Override
                    public int characteristics() {
                        return ORDERED;
                    }
                }

                class RightIterator implements Spliterator<U> {

                    @Override
                    public boolean tryAdvance(Consumer<? super U> consumer) {
                        while (true) {
                            var f = right.pollFirst();
                            if (f != null) {
                                consumer.accept(f);
                                return true;
                            }
                            var b = iterator.tryAdvance((t2) -> {
                                filterMap.apply(t2).ifPresentOrElse(right::addLast, () -> left.addLast(t2));
                            });
                            if (!b) {
                                return false;
                            }
                        }
                    }

                    @Override
                    public Spliterator<U> trySplit() {
                        return null;
                    }

                    @Override
                    public long estimateSize() {
                        return iterator.estimateSize();
                    }

                    @Override
                    public int characteristics() {
                        return ORDERED;
                    }
                }

            }

            @Override
            public <T, U> Pair<Stream<T>, Stream<U>> split(Stream<T> stream, Function<? super T, Optional<? extends U>> filterMap) {
                var it = new ChuckIterator<T, U>();
                it.iterator = stream.spliterator();
                it.filterMap = filterMap;
                var left = it.new LeftIterator();
                var right = it.new RightIterator();
                return new Pair<>(StreamSupport.stream(left, false), StreamSupport.stream(right, false));
            }
        };
    }

}
