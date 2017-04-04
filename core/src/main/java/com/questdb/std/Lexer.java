/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2017 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.std;

import com.questdb.misc.Chars;
import com.questdb.std.str.AbstractCharSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Lexer extends AbstractImmutableIterator<CharSequence> {
    public static final LenComparator COMPARATOR = new LenComparator();
    private static final CharSequenceHashSet whitespace = new CharSequenceHashSet();
    private final IntObjHashMap<List<CharSequence>> symbols = new IntObjHashMap<>();
    private final CharSequence floatingSequence = new FloatingSequence();
    private CharSequence next = null;
    private int _lo;
    private int _hi;
    private int _pos;
    private int _len;
    private CharSequence content;
    private CharSequence unparsed;
    private CharSequence last;

    public Lexer() {
        for (int i = 0, n = whitespace.size(); i < n; i++) {
            defineSymbol(whitespace.get(i).toString());
        }
    }

    public final void defineSymbol(String token) {
        char c0 = token.charAt(0);
        List<CharSequence> l = symbols.get(c0);
        if (l == null) {
            l = new ArrayList<>();
            symbols.put(c0, l);
        }
        l.add(token);
        l.sort(COMPARATOR);
    }

    public CharSequence getContent() {
        return content;
    }

    public void setContent(CharSequence cs) {
        setContent(cs, 0, cs == null ? 0 : cs.length());
    }

    @Override
    public boolean hasNext() {
        boolean n = next != null || unparsed != null || (content != null && _pos < _len);
        if (!n && last != null) {
            last = null;
        }
        return n;
    }

    @Override
    public CharSequence next() {

        if (unparsed != null) {
            CharSequence result = unparsed;
            unparsed = null;
            return last = result;
        }

        this._lo = this._hi;

        if (next != null) {
            CharSequence result = next;
            next = null;
            return last = result;
        }

        this._lo = this._hi = _pos;

        char term = 0;

        while (hasNext()) {
            char c = content.charAt(_pos++);
            CharSequence token;
            switch (term) {
                case 1:
                    if ((token = token(c)) != null) {
                        return last = token;
                    } else {
                        _hi++;
                    }
                    break;
                case 0:
                    switch (c) {
                        case '\'':
                            term = '\'';
                            break;
                        case '"':
                            term = '"';
                            break;
                        case '`':
                            term = '`';
                            break;
                        default:
                            if ((token = token(c)) != null) {
                                return last = token;
                            } else {
                                _hi++;
                            }
                            term = 1;
                            break;
                    }
                    break;
                case '\'':
                    switch (c) {
                        case '\'':
                            _hi += 2;
                            return last = floatingSequence;
                        default:
                            _hi++;
                            break;
                    }
                    break;
                case '"':
                    switch (c) {
                        case '"':
                            _hi += 2;
                            return last = floatingSequence;
                        default:
                            _hi++;
                            break;
                    }
                    break;
                case '`':
                    switch (c) {
                        case '`':
                            _hi += 2;
                            return last = floatingSequence;
                        default:
                            _hi++;
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
        return last = floatingSequence;
    }

    public CharSequence optionTok() {
        int blockCount = 0;
        boolean lineComment = false;
        while (hasNext()) {
            CharSequence cs = next();

            if (lineComment) {
                if (Chars.equals(cs, '\n') || Chars.equals(cs, '\r')) {
                    lineComment = false;
                }
                continue;
            }

            if (Chars.equals("--", cs)) {
                lineComment = true;
                continue;
            }

            if (Chars.equals("/*", cs)) {
                blockCount++;
                continue;
            }

            if (Chars.equals("*/", cs) && blockCount > 0) {
                blockCount--;
                continue;
            }

            if (blockCount == 0 && !whitespace.contains(cs)) {
                return cs;
            }
        }
        return null;
    }

    public int position() {
        return _lo;
    }

    public void setContent(CharSequence cs, int lo, int hi) {
        this.content = cs;
        this._pos = lo;
        this._len = hi;
        this.next = null;
        this.unparsed = null;
    }

    public void unparse() {
        unparsed = last;
    }

    private static CharSequence findToken0(char c, CharSequence content, int _pos, int _len, IntObjHashMap<List<CharSequence>> symbols) {

        List<CharSequence> l = symbols.get(c);
        if (l == null) {
            return null;
        }

        for (int i = 0, sz = l.size(); i < sz; i++) {
            CharSequence txt = l.get(i);
            int n = txt.length();
            boolean match = (n - 2) < (_len - _pos);
            if (match) {
                for (int k = 1; k < n; k++) {
                    if (content.charAt(_pos + (k - 1)) != txt.charAt(k)) {
                        match = false;
                        break;
                    }
                }
            }

            if (match) {
                return txt;
            }
        }
        return null;
    }

    private CharSequence token(char c) {
        CharSequence t = findToken0(c, content, _pos, _len, symbols);
        if (t != null) {
            _pos = _pos + t.length() - 1;
            if (_lo == _hi) {
                return t;
            } else {
                next = t;
            }
            return floatingSequence;
        } else {
            return null;
        }
    }

    private static class LenComparator implements Comparator<CharSequence> {
        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            return o2.length() - o1.length();
        }
    }

    private class FloatingSequence extends AbstractCharSequence {
        @Override
        public int length() {
            return _hi - _lo;
        }

        @Override
        public char charAt(int index) {
            return content.charAt(_lo + index);
        }
    }

    static {
        whitespace.add(" ");
        whitespace.add("\t");
        whitespace.add("\n");
        whitespace.add("\r");
    }
}