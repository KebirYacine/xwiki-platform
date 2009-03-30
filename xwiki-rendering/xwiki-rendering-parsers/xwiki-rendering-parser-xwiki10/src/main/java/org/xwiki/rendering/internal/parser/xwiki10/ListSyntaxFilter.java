/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.internal.parser.xwiki10;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("list")
public class ListSyntaxFilter extends AbstractFilter implements Initializable
{
    private static final Pattern LISTYNTAX_PATTERN =
        Pattern.compile("(^[\\p{Blank}]*+([-#*]++|[-#*iIaA1ghHkKj]++\\.)[\\p{Blank}]++[^\r\n]++([ \t]*+[\r\n]++)*+)++",
            Pattern.MULTILINE);

    private static final Pattern LISTITEMSYNTAX_PATTERN =
        Pattern.compile("^([\\p{Blank}]*+)([-#*]++|[-#*iIaA1ghHkKj]++\\.)([\\p{Blank}]++[^\r\n]++)([ \t]*+[\r\n]++)*+",
            Pattern.MULTILINE);

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(900);
    }

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = LISTYNTAX_PATTERN.matcher(content);

        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentIndex > 0) {
                before = CleanUtil.setLastNewLines(CleanUtil.setFirstNewLines(before, 2), 2);
            }

            result.append(before);
            result.append(filterList(matcher.group(0), filterContext));
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(CleanUtil.setFirstNewLines(content.substring(currentIndex), 1));

        return result.toString();
    }

    public String filterList(String content, FilterContext filterContext)
    {
        StringBuffer listResult = new StringBuffer();

        Matcher matcher = LISTITEMSYNTAX_PATTERN.matcher(content);

        int currentIndex = 0;
        char currentListSign = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentIndex > 0) {
                before = CleanUtil.setFirstNewLines(before, 1);
            }

            StringBuffer listItemResult = new StringBuffer();
            listItemResult.append(matcher.group(1));

            String listSigns = matcher.group(2);
            char listSign = listSigns.charAt(0);

            String listString;
            String listStyle = "";

            if (listSign == '#') {
                listString = StringUtils.repeat("1", listSigns.length()) + ".";
            } else if (listSign == '1') {
                listString = listSigns;
            } else {
                if (listSign == '-') {
                    listStyle = "square";
                    listString = StringUtils.repeat("*", listSigns.length());
                } else if (listSign == 'a') {
                    listStyle = "lower-alpha";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'A') {
                    listStyle = "upper-alpha";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'i') {
                    listStyle = "lower-roman";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'I') {
                    listStyle = "upper-roman";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'g') {
                    listStyle = "lower-greek";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'h') {
                    listStyle = "hiragana";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'H') {
                    listStyle = "hiragana-iroha";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'k') {
                    listStyle = "katakana";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'K') {
                    listStyle = "katakana-iroha";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == 'j') {
                    listStyle = "hebrew";
                    listString = StringUtils.repeat("*", listSigns.length() - 1);
                } else if (listSign == '*') {
                    listString = listSigns;
                } else {
                    // This should never append
                    getLogger().error("Unknown list sign: " + listSign);

                    listString = StringUtils.repeat("*", listSigns.length());
                }
            }

            if (listSign != currentListSign) {
                if (currentListSign != 0 && currentIndex > 0) {
                    before = CleanUtil.setLastNewLines(before, 2);
                }

                if (listStyle.length() > 0) {
                    listItemResult.append(filterContext.addProtectedContent(MessageFormat.format(
                        "(% style=\"list-style-type: {0}\" %)\n", listStyle)));
                }
            }
            listItemResult.append(filterContext.addProtectedContent(listString));

            listItemResult.append(matcher.group(3));

            listResult.append(before);
            listResult.append(listItemResult);

            currentListSign = listSign;
        }

        if (currentIndex == 0) {
            return content;
        }

        listResult.append(content.substring(currentIndex));

        return listResult.toString();
    }
}
