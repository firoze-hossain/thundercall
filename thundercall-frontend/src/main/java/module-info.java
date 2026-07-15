module com.roze.thundercall {
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.base;
    requires org.controlsfx.controls;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.lang3;
    requires org.kordamp.ikonli.javafx;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.kordamp.ikonli.fontawesome5;
    requires static lombok;
    requires org.json;
    requires java.logging;
    requires java.scripting;
    opens com.roze.thundercall.ui to javafx.graphics, javafx.fxml;
    opens com.roze.thundercall.ui.controllers to javafx.fxml, javafx.base;
    opens com.roze.thundercall.ui.models to javafx.base;
    exports com.roze.thundercall.ui.models to javafx.base, com.fasterxml.jackson.databind;

    exports com.roze.thundercall.ui;
    exports com.roze.thundercall.ui.enums;
    opens com.roze.thundercall.ui.enums to javafx.fxml, javafx.graphics;
}