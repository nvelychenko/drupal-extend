# drupal-extend

![Build](https://github.com/nvelychenko/drupal-extend/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)

The plugin is enhancement for your Drupal development experience.

## Features
* Autocomplete for Content/Config Entities Storage (eck support)

* Fields autocomplete (default configuration directory points to the `config/sync` directory, you can change it inside plugin settings. <kbd>Settings</kbd> -> <kbd>PHP</kbd> -> <kbd>Drupal Extend</kbd>)

* Field properties autocomplete e.g. `$node->get('field_user')->en|` it will autocomplete **entity** in this case.

* Render element types autocomplete and their properties.

* References for Storages and Fields (Ctrl + Click)

* Reference for Render element type.

* TypeProvider for `$storage->load/loadMultiple/loadByProperties` e.g. IDE will know what object(s) is returned by any of these methods.

* TypeProvider for static `Node::load/create/loadMultiple` methods

* TypeProvider for `\Drupal::service('database')->|`. So ide know what object is returned by this `::service` method.

* WIP Autocomplete for `#theme`

* Autocomplete for Render Element and its properties e.g. `#type' => 'checkbox', '#tit|`

* Render element type highlight (Annotator)

* Fields autocomplete in content entity queries and entity storages autocomplete in `\Drupal::entityQuery()`

* (Optional) Line marker for render elements. It is disabled by default to enable it go to <kbd>Settings</kbd> -> <kbd>Editor</kbd> -> <kbd>Gutter Icons</kbd> -> <kbd>Render Element Type</kbd>

![Render Element line marker example](./assets/line_marker_example.png)

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "drupal-extend"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/nvelychenko/drupal-extend/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
