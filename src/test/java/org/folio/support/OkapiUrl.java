package org.folio.support;

import java.net.URI;
import lombok.SneakyThrows;

public class OkapiUrl {
  private final String url;

  public OkapiUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return url;
  }

  @SneakyThrows
  public URI asURI() {
    return new URI(url);
  }

  @SneakyThrows
  public URI asURI(String path) {
    return new URI(url + path);
  }
}
