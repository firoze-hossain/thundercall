package com.roze.thundercall.ui.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One reusable script snippet saved via the Scripts editor's right-click
 * "Save to Package Library" menu (Postman-style). Stored locally on this
 * machine — see {@code PackageLibraryService} — since there is no backend
 * endpoint for a package library today.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScriptPackage {
    private String id;
    private String name;
    private String description;
    private String script;

    /** Used by ChoiceDialog<ScriptPackage> to show just the package name. */
    @Override
    public String toString() {
        return name == null || name.isBlank() ? "(unnamed package)" : name;
    }
}
