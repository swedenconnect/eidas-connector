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
package se.swedenconnect.eidas.connector.authn.ui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.ext.saml2mdui.Logo;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The display names and logotypes of a Swedish Service Provider, read from its metadata.
 * <p>
 * The display names are taken from the {@code mdui:UIInfo} element, then from {@code OrganizationDisplayName} and last
 * from {@code OrganizationName}. For each language, the first name found is used.
 * </p>
 *
 * @author Martin Lindström
 */
public class ServiceProviderUiInfo {

  /** The key used for a display name without a language. */
  public static final @NonNull String NO_LANG = "nolang";

  /** The display names, keyed by language. */
  private final @NonNull Map<String, String> displayNames;

  /** The logotypes. */
  private final @NonNull List<Logotype> logotypes;

  /**
   * Constructor.
   *
   * @param metadata the metadata of the Service Provider, may be {@code null}
   */
  public ServiceProviderUiInfo(final @Nullable EntityDescriptor metadata) {

    final UIInfo uiInfo = Optional.ofNullable(metadata)
        .map(m -> m.getSPSSODescriptor(SAMLConstants.SAML20P_NS))
        .map(SPSSODescriptor::getExtensions)
        .map(e -> e.getUnknownXMLObjects(UIInfo.DEFAULT_ELEMENT_NAME))
        .filter(l -> !l.isEmpty())
        .map(List::getFirst)
        .map(UIInfo.class::cast)
        .orElse(null);

    final Map<String, String> displayNamesMap = new HashMap<>();

    if (uiInfo != null) {
      uiInfo.getDisplayNames()
          .forEach(d -> displayNamesMap.put(Optional.ofNullable(d.getXMLLang()).orElse(NO_LANG), d.getValue()));
      this.logotypes = uiInfo.getLogos().stream()
          .map(Logotype::new)
          .filter(l -> l.getUrl() != null)
          .toList();
    }
    else {
      this.logotypes = Collections.emptyList();
    }

    if (metadata != null && metadata.getOrganization() != null) {
      metadata.getOrganization().getDisplayNames()
          .forEach(dn -> displayNamesMap.putIfAbsent(Optional.ofNullable(dn.getXMLLang()).orElse(NO_LANG),
              dn.getValue()));
      metadata.getOrganization().getOrganizationNames()
          .forEach(on -> displayNamesMap.putIfAbsent(Optional.ofNullable(on.getXMLLang()).orElse(NO_LANG),
              on.getValue()));
    }

    this.displayNames = Collections.unmodifiableMap(displayNamesMap);
  }

  /**
   * Gets the display names, keyed by language.
   *
   * @return the display names
   */
  public @NonNull Map<String, String> getDisplayNames() {
    return this.displayNames;
  }

  /**
   * Gets the display name for the given language, or the name without a language.
   *
   * @param languageTag the language tag
   * @return the display name, or {@code null}
   */
  public @Nullable String getDisplayName(final @NonNull String languageTag) {
    return this.displayNames.getOrDefault(languageTag, this.displayNames.get(NO_LANG));
  }

  /**
   * Gets the first logotype that matches the predicate.
   *
   * @param predicate the predicate
   * @return the logotype, or {@code null}
   */
  public @Nullable Logotype getLogotype(final @NonNull Predicate<Logotype> predicate) {
    Objects.requireNonNull(predicate, "predicate must not be null");
    return this.logotypes.stream()
        .filter(predicate)
        .findFirst()
        .orElse(null);
  }

  /**
   * A logotype of the Service Provider.
   */
  public static class Logotype {

    /** The URL of the logotype. */
    private final @Nullable String url;

    /** The height in pixels. */
    private final @Nullable Integer height;

    /** The language. */
    private final @Nullable String language;

    /**
     * Constructor.
     *
     * @param logo the metadata {@code Logo} element
     */
    public Logotype(final @NonNull Logo logo) {
      this.url = logo.getURI();
      this.height = logo.getHeight();
      this.language = logo.getXMLLang();
    }

    /**
     * Gets the URL of the logotype.
     *
     * @return the URL
     */
    public @Nullable String getUrl() {
      return this.url;
    }

    /**
     * Gets the height in pixels.
     *
     * @return the height, or {@code null}
     */
    public @Nullable Integer getHeight() {
      return this.height;
    }

    /**
     * Gets the language.
     *
     * @return the language, or {@code null}
     */
    public @Nullable String getLanguage() {
      return this.language;
    }

  }

}
