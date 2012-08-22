package com.github.uchan_nos.c_helper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IQualifierType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;

/**
 * 便利関数群.
 * @author uchan
 */
public class Util {
    /**
     * ファイル内容をすべて読み込み、文字列として返す.
     * @param file 読み込むファイル
     * @param charsetName ファイルのエンコーディング
     * @return ファイルの内容
     * @throws IOException
     */
    public static String readFileAll(File file, String charsetName) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charsetName));
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            sb.append((char)c);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * ファイル内容をすべて読み込み、文字列として返す.
     * ファイルはUTF-8でエンコーディングされていると仮定する.
     * @param file 読み込むファイル
     * @return ファイルの内容
     * @throws IOException
     */
    public static String readFileAll(File file) throws IOException {
        return readFileAll(file, "UTF-8");
    }

    /**
     * コントロールフローグラフの頂点集合をソートした集合を返す.
     * @param vertices 頂点集合
     * @return ソート済み頂点集合
     */
    public static Set<CFG.Vertex> sort(Set<CFG.Vertex> vertices) {
        /**
         * コントロールフローグラフの頂点の比較器.
         * @author uchan
         */
        class VertexComparator implements Comparator<CFG.Vertex> {
            @Override
            public int compare(CFG.Vertex o1, CFG.Vertex o2) {
                if (o1.getASTNode() == null && o2.getASTNode() == null) {
                    return 0;
                } else if (o1.getASTNode() == null && o2.getASTNode() != null) {
                    return -1;
                } else if (o1.getASTNode() != null && o2.getASTNode() == null) {
                    return 1;
                } else {
                    IASTNode n1 = o1.getASTNode();
                    IASTNode n2 = o2.getASTNode();
                    return n1.getFileLocation().getNodeOffset() - n2.getFileLocation().getNodeOffset();
                }
            }
        }

        TreeSet<CFG.Vertex> sorted = new TreeSet<CFG.Vertex>(new VertexComparator());
        sorted.addAll(vertices);
        return sorted;
    }

    /**
     * 指定された名前の到達定義を取得する.
     */
    public static Set<AssignExpression> getAssigns(AssignExpression[] assigns, BitSet rd, String name) {
        Set<AssignExpression> result = new HashSet<AssignExpression>();
        for (AssignExpression ae : assigns) {
            if (rd.get(ae.getId())) {
                IASTNode lhs = ae.getLHS();
                String nameOfLhs = null;
                if (lhs instanceof IASTIdExpression) {
                    nameOfLhs = ((IASTIdExpression)lhs).getName().toString();
                } else if (lhs instanceof IASTName) {
                    nameOfLhs = ((IASTName)lhs).toString();
                }

                if (name.equals(nameOfLhs)) {
                    result.add(ae);
                }
            }
        }
        return result;
    }

    /**
     * 指定されたビット数だけを取り出す.
     * @return ビットマスク後の値（非負整数）
     */
    public static BigInteger maskBits(BigInteger value, int bits) {
        BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
        return value.and(mask);
    }

    /**
     * 与えられた整数の指定されたbits以上のビットを切り落とす.
     * 切り落とした後の整数の bits-1 ビット目が1なら、負数に変換する.
     *
     * value = 416 (0x01a0), bits = 8
     * のとき、結果は
     * -96 (0xffa0)
     * となる.
     *
     * @param value 切り落とし対象の整数
     * @param bits 切り落とす位置
     * @return bits 以上のビットを切り落とした整数
     */
    public static BigInteger cutBits(BigInteger value, int bits) {
        BigInteger mask = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
        value = value.and(mask);
        if (value.testBit(bits - 1)) {
            value = value.or(mask.not());
        }
        return value;
    }

    /**
     * 行頭からのオフセットを返す.
     * @param source ソースコード
     * @param offset ソースコード先頭からのオフセット
     * @return 行頭からのオフセット
     */
    public static int calculateColumnNumber(String source, int offset, String lineDelimiter) {
        int prevLF = source.lastIndexOf(lineDelimiter, offset);
        if (prevLF >= 0) {
            return offset - prevLF - lineDelimiter.length();
        }
        return offset;
    }

    /**
     * 行頭からのオフセットを返す.
     * @param source ソースコード
     * @param offset ソースコード先頭からのオフセット
     * @return 行頭からのオフセット
     */
    public static int calculateColumnNumbeer(IDocument source, int offset) throws BadLocationException {
        return offset - source.getLineOffset(source.getLineOfOffset(offset));
    }

    /**
     * 文字列にパターンが出現する回数を返す.
     * @param s 検索対象の文字列
     * @param pattern 検索するパターン
     * @return パターンの出現回数
     */
    public static int countMatches(String s, Pattern pattern) {
        int count = 0;
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            ++count;
        }
        return count;
    }

    /**
     * 指定されたノードの raw signature を返す.
     * もし指定されたノードがnullなら空文字列""を返す.
     * @param node raw signature を取得したいノード
     * @return raw signature
     */
    public static String getRawSignature(IASTNode node) {
        return node != null ? node.getRawSignature() : "";
    }

    public interface CharPredicate {
        boolean evaluate(char c);
    }

    /**
     * 指定された文字列から指定された述語が成り立つ文字を探し、そのインデックスを返す.
     * 述語を渡せる String#indexOf と考えればよい.
     * @param s
     * @param p
     * @param fromIndex
     * @return
     */
    public static int indexOf(String s, CharPredicate p, int fromIndex) {
        for (int i = fromIndex >= 0 ? fromIndex : 0; i < s.length(); ++i) {
            if (p.evaluate(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 指定された文字列から指定された述語が成り立つ文字を探し、そのインデックスを返す.
     * 述語を渡せる String#indexOf と考えればよい.
     * commentsにnull以外が渡された場合、
     * commentsに含まれるコメント以外で初めて現れた文字のインデックスを返す.
     * @param s
     * @param p
     * @param fromIndex
     * @param comments
     * @param commentOffset
     * @return
     */
    public static int indexOf(String s, CharPredicate p, int fromIndex, IASTComment[] comments, final int commentOffset) {
        if (comments == null) {
            return indexOf(s, p, fromIndex);
        }

        // コメントの位置順にソート
        Arrays.sort(comments, new Comparator<IASTComment>() {
            @Override
            public int compare(IASTComment o1, IASTComment o2) {
                IASTFileLocation l1 = o1.getFileLocation();
                IASTFileLocation l2 = o2.getFileLocation();
                if (l1.getNodeOffset() == l2.getNodeOffset()) {
                    return l1.getNodeLength() - l2.getNodeLength();
                } else {
                    return l1.getNodeOffset() - l2.getNodeOffset();
                }
            }
        });

        final TwoComparator<IASTComment, Integer> comparator = new TwoComparator<IASTComment, Integer>() {
            @Override
            public int compare(IASTComment a, Integer b) {
                final int offset = commentOffset + a.getFileLocation().getNodeOffset();
                final int length = a.getFileLocation().getNodeLength();
                if (offset <= b && b < offset + length) {
                    return 0;
                } else if (offset + length <= b) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };

        while (true) {
            int pos = indexOf(s, p, fromIndex);
            if (pos < 0) {
                return pos;
            }

            int commentPos = binarySearch(comments, pos, comparator);
            if (commentPos < 0) {
                return pos;
            }
            fromIndex = pos + 1;
        }
    }

    public interface TwoComparator<T, U> {
        int compare(T a, U b);
    }

    public static <T,U> int binarySearch(T[] a, U key, TwoComparator<T,U> c) {
        int low = 0;
        int high = a.length - 1;
        int mid = 0;

        while (low <= high) {
            mid = (high + low) / 2;
            int res = c.compare(a[mid], key);
            if (res == 0) {
                return mid;
            } else if (res > 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return -low - 1;
    }

    public static boolean isIBasicType(IType type, IBasicType.Kind kind) {
        return type instanceof IBasicType
                && ((IBasicType) type).getKind() == kind;
    }

    /**
     * 与えられたノードから IASTName を探して返す.
     * 与えられたノードが IASTIdExpression または IASTName 以外なら null を返す.
     * @param node 検索対象のノード
     * @return 検索された IASTName. ヒットしなければ null.
     */
    public static IASTName getName(IASTNode node) {
        if (node instanceof IASTIdExpression) {
            return ((IASTIdExpression) node).getName();
        } else if (node instanceof IASTName) {
            return (IASTName) node;
        }
        return null;
    }

    public static boolean equals(char[] s1, String s2) {
        return String.valueOf(s1).equals(s2);
    }

    public static boolean equals(String s1, char[] s2) {
        return equals(s2, s1);
    }

    /**
     * 指定された型から型修飾子 IQualifierType を取り除いた型を返す.
     * @param type 取り除く対象の型
     * @return 型修飾子を取り除いた型
     */
    public static IType removeQualifier(IType type) {
        while (type instanceof IQualifierType) {
            type = ((IQualifierType) type).getType();
        }
        return type;
    }
}
