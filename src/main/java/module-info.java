module io.infinitytools.wml {
  requires transitive javafx.controls;
  requires transitive javafx.graphics;
  requires javafx.fxml;
  requires org.json;
  requires org.tinylog.impl;
  requires org.tinylog.api;

  opens io.infinitytools.wml.gui to javafx.fxml;
  exports io.infinitytools.wml.gui;
  exports io.infinitytools.wml.mod;
}
