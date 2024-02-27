<?php

namespace PHPSTORM_META {

  override(\Drupal\Core\DependencyInjection\ClassResolverInterface::getInstanceFromDefinition(0), type(0));

  expectedReturnValues(
    \Drupal\Core\Entity\EntityInterface::save(),
    \SAVED_NEW,
    \SAVED_UPDATED
  );

  expectedArguments(
    \Drupal\Core\Entity\EntityViewBuilderInterface::view(),
    2,
    \Drupal\Core\Language\LanguageInterface::LANGCODE_NOT_SPECIFIED,
    \Drupal\Core\Language\LanguageInterface::LANGCODE_NOT_APPLICABLE,
    \Drupal\Core\Language\LanguageInterface::LANGCODE_DEFAULT,
    \Drupal\Core\Language\LanguageInterface::LANGCODE_SITE_DEFAULT
  );

  expectedArguments(
    \Drupal\Core\Messenger\MessengerInterface::addMessage(),
    1,
    \Drupal\Core\Messenger\MessengerInterface::TYPE_STATUS,
    \Drupal\Core\Messenger\MessengerInterface::TYPE_WARNING,
    \Drupal\Core\Messenger\MessengerInterface::TYPE_ERROR
  );

  expectedArguments(
    \Drupal\Core\File\FileSystemInterface::prepareDirectory(),
    1,
    \Drupal\Core\File\FileSystemInterface::CREATE_DIRECTORY,
    \Drupal\Core\File\FileSystemInterface::MODIFY_PERMISSIONS
  );

  registerArgumentsSet('file_system_exists_behaviour',
    \Drupal\Core\File\FileSystemInterface::EXISTS_RENAME,
    \Drupal\Core\File\FileSystemInterface::EXISTS_REPLACE,
    \Drupal\Core\File\FileSystemInterface::EXISTS_ERROR
  );

  expectedArguments(\Drupal\Core\File\FileSystemInterface::copy(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\Drupal\Core\File\FileSystemInterface::move(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\Drupal\Core\File\FileSystemInterface::saveData(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\Drupal\Core\File\FileSystemInterface::getDestinationFilename(), 1, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\file_copy(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\file_move(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\file_save_data(), 2, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\file_save_upload(), 4, argumentsSet('file_system_exists_behaviour'));
  expectedArguments(\system_retrieve_file(), 3, argumentsSet('file_system_exists_behaviour'));

}
