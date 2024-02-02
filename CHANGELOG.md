<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# drupal-extend Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/nvelychenko/drupal-extend/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.2...v0.4.0
[0.3.2]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/nvelychenko/drupal-extend/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nvelychenko/drupal-extend/compare/0.0.5...v0.1.0
[0.0.5]: https://github.com/nvelychenko/drupal-extend/commits/0.0.5
