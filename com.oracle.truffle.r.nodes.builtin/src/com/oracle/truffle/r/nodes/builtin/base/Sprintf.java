/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.IllegalFormatException;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "sprintf", kind = INTERNAL, parameterNames = {"fmt", "..."}, behavior = PURE)

public abstract class Sprintf extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(Sprintf.class);
        casts.arg("fmt").mustBe(stringValue()).asStringVector();
    }

    public abstract Object executeObject(String fmt, Object args);

    @Child private Sprintf sprintfRecursive;

    @Specialization
    protected RStringVector sprintf(RAbstractStringVector fmt, RList values) {
        return sprintf(fmt, new RArgsValuesAndNames(values.getReadonlyData(), ArgumentsSignature.empty(values.getLength())));
    }

    @Specialization
    protected RStringVector sprintf(@SuppressWarnings("unused") RAbstractStringVector fmt, @SuppressWarnings("unused") RNull x) {
        return RDataFactory.createEmptyStringVector();
    }

    @Specialization
    protected String sprintf(String fmt, @SuppressWarnings("unused") RMissing x) {
        return fmt;
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, RMissing x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, int x) {
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, int x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, byte x) {
        return format(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractIntVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractIntVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, double x) {
        char f = Character.toLowerCase(firstFormatChar(fmt));
        if (f == 'x' || f == 'd') {
            if (Math.floor(x) == x) {
                return format(fmt, (long) x);
            }
            throw error(RError.Message.INVALID_FORMAT_DOUBLE, fmt);
        }
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, double x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractDoubleVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = sprintf(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractDoubleVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected String sprintf(String fmt, String x) {
        return format(fmt, x);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected String sprintf(RAbstractStringVector fmt, String x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractStringVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractLogicalVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RAbstractLogicalVector x) {
        String[] r = new String[x.getLength()];
        for (int k = 0; k < r.length; k++) {
            r[k] = format(fmt, x.getDataAt(k));
        }
        return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = "fmtLengthOne(fmt)")
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RAbstractStringVector x) {
        return sprintf(fmt.getDataAt(0), x);
    }

    private static int maxLengthAndConvertToScalar(Object[] values) {
        int length = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof RAbstractVector) {
                int vecLength = ((RAbstractVector) values[i]).getLength();
                if (vecLength == 0) {
                    // result will be empty character vector in this case, as in:
                    // sprintf("%d %d", as.integer(c(7,42)), integer())
                    return 0;
                } else {
                    if (vecLength == 1) {
                        values[i] = ((RAbstractVector) values[i]).getDataAtAsObject(0);
                    }
                    length = Math.max(vecLength, length);
                }
            } else {
                length = Math.max(1, length);
            }
        }
        return length;
    }

    private static Object[] createSprintfArgs(Object[] values, int index, int maxLength) {
        Object[] sprintfArgs = new Object[values.length];
        for (int i = 0; i < sprintfArgs.length; i++) {
            if (values[i] instanceof RAbstractVector) {
                sprintfArgs[i] = ((RAbstractVector) values[i]).getDataAtAsObject(index % maxLength);
            } else {
                sprintfArgs[i] = values[i];
            }
        }
        return sprintfArgs;
    }

    @Specialization(guards = {"!oneElement(args)", "hasNull(args)"})
    protected RStringVector sprintf(@SuppressWarnings("unused") Object fmt, @SuppressWarnings("unused") RArgsValuesAndNames args) {
        return RDataFactory.createEmptyStringVector();
    }

    @Specialization(guards = {"!oneElement(args)", "!hasNull(args)"})
    @TruffleBoundary
    protected RStringVector sprintf(String fmt, RArgsValuesAndNames args) {
        Object[] values = args.getArguments();
        int maxLength = maxLengthAndConvertToScalar(values);
        if (maxLength == 0) {
            if (values.length > 0) {
                return RDataFactory.createEmptyStringVector();
            } else {
                return RDataFactory.createStringVector(fmt);
            }
        } else {
            String[] r = new String[maxLength];
            for (int k = 0; k < r.length; k++) {
                Object[] sprintfArgs = createSprintfArgs(values, k, maxLength);
                r[k] = format(fmt, sprintfArgs);
            }
            return RDataFactory.createStringVector(r, RDataFactory.COMPLETE_VECTOR);

        }
    }

    @Specialization(guards = "oneElement(args)")
    protected Object sprintfOneElement(String fmt, RArgsValuesAndNames args) {
        if (sprintfRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sprintfRecursive = insert(SprintfNodeGen.create());
        }
        return sprintfRecursive.executeObject(fmt, args.getArgument(0));
    }

    @Specialization(guards = {"!oneElement(args)", "!hasNull(args)"})
    @TruffleBoundary
    protected RStringVector sprintf(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        if (fmt.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            String[] data = new String[fmt.getLength()];
            for (int i = 0; i < data.length; i++) {
                RStringVector formatted = sprintf(fmt.getDataAt(i), args);
                assert formatted.getLength() > 0;
                data[i] = formatted.getDataAt(args.getLength() == 0 ? 0 : i % Math.min(args.getLength(), formatted.getLength()));
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Specialization(guards = {"oneElement(args)", "fmtLengthOne(fmt)"})
    protected Object sprintfOneElement(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        return sprintfOneElement(fmt.getDataAt(0), args);
    }

    @Specialization(guards = {"oneElement(args)", "!fmtLengthOne(fmt)"})
    protected Object sprintf2(RAbstractStringVector fmt, RArgsValuesAndNames args) {
        if (fmt.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            String[] data = new String[fmt.getLength()];
            for (int i = 0; i < data.length; i++) {
                Object formattedObj = sprintfOneElement(fmt.getDataAt(i), args);
                if (formattedObj instanceof String) {
                    data[i] = (String) formattedObj;
                } else {
                    RStringVector formatted = (RStringVector) formattedObj;
                    if (formatted.getLength() == 0) {
                        // Any NULL inside args causes the whole result to be empty vector
                        return formatted;
                    }
                    data[i] = formatted.getDataAt(i % formatted.getLength());
                }
            }
            return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private String format(String fmt, Object... args) {
        char[] conversions = new char[args.length];
        String format = processFormat(fmt, args, conversions);
        adjustValues(args, conversions);
        return stringFormat(format, fmt, args);
    }

    private String processFormat(String fmt, Object[] args, char[] conversions) {
        int i = 0;
        char[] cs = fmt.toCharArray();
        StringBuilder sb = new StringBuilder();
        StringBuilder tmpSb = new StringBuilder();
        int argc = 1;

        while (i < cs.length) {
            // skip up to and including next %
            while (i < cs.length && cs[i] != '%') {
                sb.append(cs[i++]);
            }
            if (i == cs.length) {
                break;
            }
            sb.append(cs[i++]);

            FormatInfo fi = extractFormatInfo(cs, i, argc);
            argc = fi.argc;
            if (fi.conversion != '%') {
                if (fi.numArg > conversions.length) {
                    throw error(Message.TOO_FEW_ARGUMENTS);
                }
                Object arg = args[fi.numArg - 1];
                if (isNA(arg)) {
                    fi.conversion = 's';
                    fi.padZero = false;
                    fi.alwaysSign = false;
                    args[fi.numArg - 1] = "NA";
                }
                // which argument to print
                sb.append(intString(fi.numArg)).append('$');
                if (Character.toLowerCase(fi.conversion) == 'g' && arg instanceof Number && !(arg instanceof Double)) {
                    // Only for g/G type and numeric value other than doubles (including logical)
                    // the type is converted to 'd', which discards any decimal points even if
                    // requested by the formatting command. Otherwise method 'adjustValues' takes
                    // care of converting the value to Double (e.g. for 'f').
                    fi.conversion = 'd';
                }
                if (Character.toLowerCase(fi.conversion) == 'g' && arg instanceof Double) {
                    // C unlike Java removes trailing zeroes for g/G. To simulate this,
                    // we convert the number to a string here like for NAs
                    tmpSb.setLength(0);
                    tmpSb.append('%');
                    fi.appendFlags(args, tmpSb);
                    tmpSb.append(fi.conversion);
                    String formatted = String.format(tmpSb.toString(), arg);
                    if (formatted.length() > 0) {
                        int trailingZeroesIdx = formatted.length() - 1;
                        boolean removedZeroes = false;
                        while (trailingZeroesIdx >= 0 && formatted.charAt(trailingZeroesIdx) == '0') {
                            trailingZeroesIdx--;
                            removedZeroes = true;
                        }
                        if (removedZeroes && trailingZeroesIdx >= 0 && formatted.charAt(trailingZeroesIdx) == '.') {
                            trailingZeroesIdx--;
                        }
                        formatted = formatted.substring(0, trailingZeroesIdx + 1);
                    }
                    fi.conversion = 's';
                    args[fi.numArg - 1] = formatted;
                } else {
                    fi.appendFlags(args, sb);
                    conversions[fi.numArg - 1] = fi.conversion;
                }
            }
            sb.append(fi.conversion);
            i = fi.nextChar;
        }

        return sb.toString();
    }

    private static boolean isNA(Object val) {
        // TODO: not correct for raw value that happens to be logical NA
        return (val instanceof Integer && RRuntime.isNA((Integer) val)) ||
                        (val instanceof Double && RRuntime.isNA((Double) val)) ||
                        (val instanceof String && RRuntime.isNA((String) val)) ||
                        (val instanceof Byte && RRuntime.isNA((Byte) val));
    }

    private static int intValue(Object o) {
        if (o instanceof Double) {
            return ((Double) o).intValue();
        } else if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            throw fail("unexpected type");
        }
    }

    @TruffleBoundary
    private static String intString(int x) {
        return Integer.toString(x);
    }

    private static char firstFormatChar(String fmt) {
        int pos = 0;
        char f;
        for (f = '\0'; f == '\0'; f = fmt.charAt(pos + 1)) {
            pos = fmt.indexOf('%', pos);
            if (pos == -1 || pos >= fmt.length() - 1) {
                return '\0';
            }
            if (fmt.charAt(pos + 1) == '%') {
                continue;
            }
            while (!Character.isLetter(fmt.charAt(pos + 1)) && pos < fmt.length() - 1) {
                pos++;
            }
        }
        return f;
    }

    @TruffleBoundary
    private static String stringFormat(String format, String originalFormat, Object[] args) {
        try {
            return String.format((Locale) null, format, args);
        } catch (IllegalFormatException ex) {
            String message = String.format("Error in Java format String '%s', R format string was '%s'.", format, originalFormat);
            throw RInternalError.shouldNotReachHere(ex, message);
        }
    }

    private void adjustValues(Object[] args, char[] conversions) {
        for (int i = 0; i < args.length; i++) {
            if (conversions[i] == 0) {
                continue;
            }
            boolean wrongConversion = false;
            char c = conversions[i];
            char lowerC = Character.toLowerCase(c);
            if (c == 'd' || c == 'i' || c == 'o' || lowerC == 'x') {
                if (args[i] instanceof Double) {
                    double doubleVal = (Double) args[i];
                    if (doubleVal == (int) doubleVal) {
                        args[i] = (int) doubleVal;
                    } else {
                        wrongConversion = false;
                    }
                } else if (args[i] instanceof Byte) {
                    args[i] = ((Byte) args[i]).intValue();
                }
            } else if (lowerC == 'f' || lowerC == 'g' || lowerC == 'e' || lowerC == 'a') {
                if (args[i] instanceof Number) {
                    args[i] = ((Number) args[i]).doubleValue();
                } else {
                    wrongConversion = true;
                }
            } else if (conversions[i] == 's') {
                if (args[i] instanceof Byte) {
                    // TODO: this will be wrong if the type was actually raw
                    args[i] = RRuntime.logicalToString((Byte) args[i]);
                } else if (args[i] instanceof Double) {
                    double doubleVal = (Double) args[i];
                    if (doubleVal == (int) doubleVal) {
                        args[i] = Integer.toString((int) doubleVal);
                    } else {
                        args[i] = Double.toString(doubleVal);
                    }
                } else {
                    args[i] = args[i].toString();
                }
            }
            if (wrongConversion) {
                if (args[i] instanceof Integer) {
                    throw error(Message.INVALID_FORMAT_INTEGER, conversions[i]);
                } else if (args[i] instanceof Double) {
                    throw error(Message.INVALID_FORMAT_DOUBLE, conversions[i]);
                } else if (args[i] instanceof Byte) {
                    throw error(Message.INVALID_FORMAT_LOGICAL, conversions[i]);
                } else if (args[i] instanceof String) {
                    throw error(Message.INVALID_FORMAT_STRING, conversions[i]);
                }
            }
        }
    }

    //
    // format info parsing
    //

    private static class FormatInfo {
        char conversion;
        /**
         * If set to non-negative value, gives the desired width.
         */
        int width = -1;
        /**
         * If set to non-negative value, gives the desired precision.
         */
        int precision = -1;
        boolean adjustLeft;
        boolean alwaysSign;
        boolean spacePrefix;
        boolean padZero;
        boolean alternate;
        int numArg;
        boolean widthIsArg;
        /**
         * Indicates that the precision is not a constant, but determined by some other argument.
         */
        boolean precisionIsArg;
        int nextChar;
        int argc;

        public void appendFlags(Object[] args, StringBuilder buffer) {
            int w = 0;
            int p = 0;
            // take care of width/precision being defined by args
            if (width >= 0 || widthIsArg) {
                w = widthIsArg ? intValue(args[width - 1]) : width;
            }
            if (precision >= 0 || precisionIsArg) {
                p = precisionIsArg ? intValue(args[precision - 1]) : precision;
            }
            if (adjustLeft) {
                buffer.append('-');
            }
            if (alwaysSign) {
                buffer.append('+');
            }
            if (alternate) {
                buffer.append('#');
            }
            if (padZero) {
                buffer.append('0');
            }
            if (spacePrefix) {
                buffer.append(' ');
            }
            // width and precision
            if (width >= 0 || widthIsArg) {
                buffer.append(intString(w));
            }
            if (precision >= 0 || precisionIsArg) {
                buffer.append('.').append(intString(p));
            }
        }
    }

    //@formatter:off
    /**
     * The grammar understood by the format info extractor is as follows. Note that the
     * leading {@code %} has already been consumed in the caller and is not given in the
     * grammar.
     *
     * formatInfo        = '%'
     *                   | arg? (widthAndPrecision | '-' | '+' | ' ' | '0' | '#')* conversion
     * arg               = number '$'
     * widthAndPrecision = oneWidth
     *                   | number '.' number
     *                   | number '.' argWidth
     *                   | argWidth '.' number
     * oneWidth          = number
     *                   | argWidth
     * argWidth          = '*' arg?
     * conversion        = < one of the conversion characters, save % >
     */
    //@formatter:on
    private static FormatInfo extractFormatInfo(char[] cs, int i, int argc) {
        int j = i;
        FormatInfo fi = new FormatInfo();
        fi.argc = argc;
        char c = cs[j];
        // finished if % is the conversion
        if (c != '%') {
            // look ahead for a $ (indicates arg)
            if (isNumeric(c) && lookahead(cs, j, '$')) {
                fi.numArg = number(cs, j, fi);
                j = fi.nextChar + 1; // advance past $
                c = cs[j];
            }
            // now loop until the conversion is found
            while (!isConversion(c)) {
                switch (c) {
                    case '-':
                        fi.adjustLeft = true;
                        j++;
                        break;
                    case '+':
                        fi.alwaysSign = true;
                        j++;
                        break;
                    case ' ':
                        fi.spacePrefix = true;
                        j++;
                        break;
                    case '0':
                        fi.padZero = true;
                        j++;
                        break;
                    case '#':
                        fi.alternate = true;
                        j++;
                        break;
                    case '*':
                        widthAndPrecision(cs, j, fi);
                        j = fi.nextChar;
                        break;
                    default:
                        // it can still be a widthAndPrecision if a number is given
                        if (isNumeric(c)) {
                            widthAndPrecision(cs, j, fi);
                            j = fi.nextChar;
                        } else if (c == '.') {
                            // apparently precision can be specified without width as well, but in
                            // such case the '0' prefix if any should be interpreted as width...
                            if (fi.padZero) {
                                fi.padZero = false;
                            }
                            oneWidth(cs, j + 1, fi, false);
                            j = fi.nextChar;
                        } else {
                            throw fail("problem with format expression");
                        }
                }
                c = cs[j];
            }
        }
        fi.conversion = c;
        if (c == 'i') {
            // they seem to be equivalent but 'i' is not handled correctly by the java formatter
            fi.conversion = 'd';
        }
        // if precision specified for integer decimal, do instead zero padding
        // e.g. '%.2d' -> '%02f'
        if (fi.precision > 0 && fi.conversion == 'd') {
            fi.padZero = true;
            fi.width = fi.precision;
            fi.precision = -1;
        }
        fi.nextChar = j + 1;
        if (fi.numArg == 0 && c != '%') {
            // no arg explicitly given, use args array
            fi.numArg = fi.argc++;
        }
        return fi;
    }

    private static void widthAndPrecision(char[] cs, int i, FormatInfo fi) {
        int j = i;
        oneWidth(cs, j, fi, true);
        j = fi.nextChar;
        if (cs[j] == '.') {
            oneWidth(cs, j + 1, fi, false);
        }
    }

    private static void oneWidth(char[] cs, int i, FormatInfo fi, boolean width) {
        int j = i;
        int n;
        if (isNumeric(cs[j])) {
            n = number(cs, j, fi);
            j = fi.nextChar;
        } else if (cs[j] == '*') {
            assert cs[j] == '*';
            if (width) {
                fi.widthIsArg = true;
            } else {
                fi.precisionIsArg = true;
            }
            j++;
            if (isNumeric(cs[j])) {
                n = number(cs, j, fi);
                j = fi.nextChar;
                assert cs[j] == '$';
                fi.nextChar = ++j;
            } else {
                n = fi.argc++;
            }
        } else {
            if (!isConversion(cs[j])) {
                throw RError.error(RError.NO_CALLER, Message.UNRECOGNIZED_FORMAT, new String(cs));
            }
            n = 0;
        }
        if (width) {
            fi.width = n;
        } else {
            fi.precision = n;
        }
        fi.nextChar = j;
    }

    private static boolean isConversion(char c) {
        return "aAdifeEgGosxX".indexOf(c) != -1;
    }

    private static boolean isNumeric(char c) {
        return c >= 48 && c <= 57;
    }

    private static int number(char[] cs, int i, FormatInfo fi) {
        int j = i;
        int num = cs[j++] - '0';
        while (isNumeric(cs[j])) {
            num = 10 * num + (cs[j++] - '0');
        }
        fi.nextChar = j;
        return num;
    }

    private static boolean lookahead(char[] cs, int i, char c) {
        int j = i;
        while (!isConversion(cs[j])) {
            if (cs[j++] == c) {
                return true;
            }
        }
        return false;
    }

    protected boolean fmtLengthOne(RAbstractStringVector fmt) {
        return fmt.getLength() == 1;
    }

    protected boolean oneElement(RArgsValuesAndNames args) {
        return args.getLength() == 1;
    }

    protected boolean hasNull(RArgsValuesAndNames args) {
        for (int i = 0; i < args.getLength(); i++) {
            if (args.getArgument(i) == RNull.instance) {
                return true;
            }
        }

        return false;
    }

    @TruffleBoundary
    private static IllegalStateException fail(String message) {
        throw new IllegalStateException(message);
    }
}
