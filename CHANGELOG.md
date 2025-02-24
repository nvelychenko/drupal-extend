<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# drupal-extend Changelog

## [Unreleased]

## [0.6.7] - 2025-02-24

### Added

- Attribute hooks autocomplete and line marker 🕺
- Permissions autocomplete
- Theme reference provider for files
- Attributes support for content/config entities, fields, themes, render/form elements.

## [0.6.5] - 2025-02-09

### Added

- Attribute hooks autocomplete and line marker 🕺
- Permissions autocomplete
- Theme reference provider for files
- Attributes support for content/config entities, fields, themes, render/form elements.

## [0.6.4] - 2024-08-19

### Fixed

- Compatibility with latest PHPStorm version

## [0.6.3] - 2024-05-20

### Fixed

- Symfony error
- \Drupal::service type provider

## [0.6.2] - 2024-03-06

Temporary disabled `\Drupa::service `type resolving, because of changes made in the latest Symfony plugin version.

## [0.6.1] - 2024-02-27

### Added

- Field autocomplete in `hasField` method.
- PhpStorm metadata file with all missing features provided by `drush generate phpstorm-metadata`, so now you can delete it without worries.

## [0.6.0] - 2024-02-15

### Added

- Added entity reference type provider e.g. $node->get('field_entity_reference_reference')->entity

## [0.5.2] - 2024-02-08

### Added

- Implemented ability to provide a type for ::create method and to autocomplete fields in it

## [0.5.1] - 2024-02-07

### Added

- Libraries autocomplete 🕺

## [0.5.0] - 2024-02-07

### Added

- Theme index. Theme and its variables autocomplete.
- Implement reference provider for themes. Implement the ability to autocomplete/reference when a theme/hook is assigned.
- Fields autocomplete for eck entities.

### Reworked

- Field type provider and its autocomplete

## [0.4.1] - 2024-02-04

### Added

- Fields autocomplete for entity query. Entities autocomplete for \Drupal::entityQuery

## [0.4.0] - 2024-02-02

### Added

- Plugin settings 🎉 (ability to choose configuration directory, disabled plugin, clear index) 
- Annotator for render element type (highlighting) 
- (optional) Line marker for render element type

### Fixed

- Completion for storage didn't work on an empty string.
- in rare cases fields where autocomplete from another entity with the same ID prefix.
- Bug with \Drupal::service

## [0.3.2] - 2024-01-31

### Added

- Autocomplete for render elements and their properties.
- Line marker for Render/Form element
- Completion priority for direct properties.
- Implemented the ability to try to find entity from storage only knowing its interface/class.
- Storage autocomplete in case Symfony is enabled, and storage is returned from service.

## [0.3.1] - 2024-01-31

### Added

- Line marker for Render/Form element
- Completion priority for direct properties.

## [0.3.0] - 2024-01-30

Added:

- Implemented ability to try to find entity from storage only knowing its interface/class.
- annotator bug
- storage autocomplete in case Symfony is enabled, and storage is returned from service.

## [0.2.0] - 2024-01-30

### Added

- Autocomplete for render elements and their properties.

## [0.1.0] - 2024-01-28

### Added

- Integration with symfony service type provider for \Drupal::service
- Config entity support (autocomplete, references)
- Field properties autocomplete e.g. $node->get('field_address')->addr| (it will autocomplete address_line1, address_line2)
- Node::load/create/loadMultiple type provider
- Almost all base fields are autocompleted now. (fields specified in a few static methods on the same class WIP)
- Storage autocomplete for getAccessControlHandler, getStorage, getViewBuilder, getListBuilder, getFormObject, getRouteProviders, hasHandler methods.
- Type provider for storage again work! But now it provider only interfaces

### Fixed

- Fixed bug related to broken entities type provider.
- Fields autocomplete in inappropriate places.

## [0.0.5]

### Added

- Index caching
- Static content entity provider e.g. Node::load()
- Fields autocomplete only knowing entity interface e.g. NodeInterface
- ECK content entity type support.

### Fixed

- Fields autocomplete was not working in certain circumstances
- Content entity provider was not working in certain circumstances
- Deprecated usage of baseDir

[Unreleased]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.7...HEAD
[0.6.7]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.5...v0.6.7
[0.6.5]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.4...v0.6.5
[0.6.4]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.3...v0.6.4
[0.6.3]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.2...v0.6.3
[0.6.2]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.1...v0.6.2
[0.6.1]: https://github.com/nvelychenko/drupal-extend/compare/v0.6.0...v0.6.1
[0.6.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.5.2...v0.6.0
[0.5.2]: https://github.com/nvelychenko/drupal-extend/compare/v0.5.1...v0.5.2
[0.5.1]: https://github.com/nvelychenko/drupal-extend/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.4.1...v0.5.0
[0.4.1]: https://github.com/nvelychenko/drupal-extend/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.2...v0.4.0
[0.3.2]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.0.5...v0.1.0
[0.0.5]: https://github.com/nvelychenko/drupal-extend/commits/v0.0.5
