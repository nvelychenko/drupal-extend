<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# drupal-extend Changelog

## [Unreleased]

## [0.2.0] - 2024-01-30

- Autocomplete for render elements and their properties.

## [0.1.0] - 2024-01-28

### Added

- Autocomplete for render elements and their properties.
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

[Unreleased]: https://github.com/nvelychenko/drupal-extend/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/nvelychenko/drupal-extend/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nvelychenko/drupal-extend/compare/0.0.5...v0.1.0
[0.0.5]: https://github.com/nvelychenko/drupal-extend/commits/0.0.5
