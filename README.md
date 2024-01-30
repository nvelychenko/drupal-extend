# drupal-extend

![Build](https://github.com/nvelychenko/drupal-extend/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23474-drupal-extend.svg)](https://plugins.jetbrains.com/plugin/23474-drupal-extend)

The plugin is enhancement for your Drupal development experience.

## Features
* Autocomplete for Content/Config Entities Storage (eck support)

* Fields autocomplete (for now config directory is hardcoded to the `config/sync`, project setting is WIP)

* Field properties autocomplete e.g. `$node->get('field_user')->en|` it will autocomplete **entity** in this case.

* References for Storages and Fields (Ctrl + Click)

* TypeProvider for `$storage->load/loadMultiple/loadByProperties` e.g. IDE will know what object(s) is returned by any of these methods.

* TypeProvider for static `Node::load/create/loadMultiple` methods

* TypeProvider for `\Drupal::service('database')->|`. So ide know what object is returned by this `::service` method.

* WIP Autocomplete for `#theme`

* Autocomplete for Render Element and its properties e.g. `#type' => 'checkbox', '#tit|`

* etc.

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "drupal-extend"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/nvelychenko/drupal-extend/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
