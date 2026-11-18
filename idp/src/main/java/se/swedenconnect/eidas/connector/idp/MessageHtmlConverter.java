/*
 * Copyright 2017-2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.eidas.connector.idp;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.NonNull;
import se.swedenconnect.spring.authnserver.message.MessageMimeType;

import java.util.List;

/**
 * Turns the text of a user message or a sign message into HTML that is safe to include in a page.
 * <p>
 * Plain text is HTML escaped, and keeps its line breaks and tabs. Markdown is rendered into HTML. HTML in the Markdown
 * is removed, and the rendered HTML is cleaned so that only an allowlist of tags, without attributes, remains.
 * </p>
 *
 * @author Martin Lindström
 */
final class MessageHtmlConverter {

  /** The CSS class of the element holding a plain text message. */
  static final String PLAIN_TEXT_CLASS = "message-text";

  /** The CSS class of the element holding a Markdown message. */
  static final String MARKDOWN_CLASS = "message-markdown";

  /** The HTML tags that may appear in a message rendered from Markdown. */
  private static final String[] ALLOWED_HTML_TAGS = { "b", "blockquote", "br", "code", "dd", "dl", "dt", "em",
      "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i", "li", "ol", "p", "pre", "strong", "table", "tbody", "td",
      "th", "thead", "tr", "ul" };

  /** The allowlist used when cleaning HTML. It removes all attributes. */
  private static final Safelist SAFELIST = new Safelist().addTags(ALLOWED_HTML_TAGS);

  /** Output settings for the cleaned HTML. */
  private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().prettyPrint(false);

  /** The Markdown parser. */
  private static final Parser MARKDOWN_PARSER;

  /** The HTML renderer for Markdown. HTML in the Markdown is not rendered. */
  private static final HtmlRenderer MARKDOWN_RENDERER;

  static {
    final MutableDataSet options = new MutableDataSet();
    options.setFrom(ParserEmulationProfile.MARKDOWN);
    options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
    options.set(HtmlRenderer.SUPPRESS_HTML, true);
    MARKDOWN_PARSER = Parser.builder(options).build();
    MARKDOWN_RENDERER = HtmlRenderer.builder(options).build();
  }

  /**
   * Turns a message text into HTML.
   *
   * @param text the message text
   * @param mimeType the MIME type of the text, {@code text/plain} or {@code text/markdown}
   * @return the HTML
   * @throws IllegalArgumentException for {@code text/html}, which is never displayed
   */
  static @NonNull String toHtml(final @NonNull String text, final @NonNull MessageMimeType mimeType) {
    return switch (mimeType) {
      case TEXT_PLAIN -> "<div class=\"%s\">%s</div>".formatted(PLAIN_TEXT_CLASS, plainTextToHtml(text));
      case TEXT_MARKDOWN -> "<div class=\"%s\">%s</div>".formatted(MARKDOWN_CLASS, markdownToHtml(text));
      case TEXT_HTML -> throw new IllegalArgumentException("HTML messages are not displayed");
    };
  }

  /**
   * Turns plain text into HTML. The text is HTML escaped, line breaks become {@code <br />} and tabs become
   * {@code &emsp;}.
   *
   * @param text the plain text
   * @return the HTML
   */
  private static String plainTextToHtml(final String text) {
    return StringEscapeUtils.escapeHtml4(text)
        .replaceAll("(\r\n|\n\r|\r|\n)", "<br />")
        .replace("\t", "&emsp;");
  }

  /**
   * Renders Markdown into HTML, and cleans the result.
   *
   * @param markdown the Markdown text
   * @return the HTML
   */
  private static String markdownToHtml(final String markdown) {
    final String html = MARKDOWN_RENDERER.render(MARKDOWN_PARSER.parse(markdown));
    return Jsoup.clean(html, "", SAFELIST, OUTPUT_SETTINGS).strip();
  }

  // Hidden constructor
  private MessageHtmlConverter() {
  }

}
