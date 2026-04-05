package net.luversof.api.stock.provider;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class InputStreamProvider {

  private final ResourceLoader resourceLoader;

  public InputStreamProvider(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public InputStream open(String location) throws IOException {
    Resource resource = resourceLoader.getResource(location);

    if (!resource.exists()) {
      throw new IllegalArgumentException("Resource not found: " + location);
    }

    return resource.getInputStream();
  }
}
