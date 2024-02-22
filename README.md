# drupal-extend

![Build](https://github.com/nvelychenko/drupal-extend/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)

The plugin is enhancement for your Drupal development experience.

## Features
* Autocomplete for Content/Config Entities Storage (eck support)
![Storage autocomplete example](./assets/storage_autocomplete.gif)

* Fields autocomplete (default configuration directory points to the `config/sync` directory, you can change it inside plugin settings. <kbd>Settings</kbd> -> <kbd>PHP</kbd> -> <kbd>Drupal Extend</kbd>)
  ![Fields autocomplete example](./assets/field_autocomplete.gif)

* Field properties autocomplete e.g. `$node->get('field_user')->en|` it will autocomplete **entity** in this case.

* Autocomplete for render element types and their properties.
  ![Render element types autocomplete](./assets/render_element_autocomplete.gif)

* Storage reference provider
  ![Storage reference](./assets/storage_reference_provider.gif)

* Fields reference provider
  ![Fields reference](./assets/field_reference.gif)

* Reference provider for Render element types and themes.
  ![Reference provider for Render element types and themes](./assets/theme_render_element_reference.gif)

* Entity Type Provider for `$storage->load/loadMultiple/loadByProperties`
  ![Entity type provider](./assets/entity_type_provider.png)

* Type Provider for static `Node::load/create/loadMultiple` methods

* Type Provider for `\Drupal::service('')`

* Autocomplete for `#theme`
  ![Theme autocomplete example](./assets/theme_autocomplete.gif)

* Render element type and theme highlight (Annotator)

* Fields autocomplete in content entity queries and entity storages autocomplete in `\Drupal::entityQuery()`

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "drupal-extend"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/nvelychenko/drupal-extend/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
