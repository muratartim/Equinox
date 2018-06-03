package Moose::Exception::BuilderMethodNotSupportedForAttribute;
our $VERSION = '2.1404';

use Moose;
extends 'Moose::Exception';
with 'Moose::Exception::Role::Attribute', 'Moose::Exception::Role::Instance';

sub _build_message {
    my $self = shift;
    blessed($self->instance)." does not support builder method '". $self->attribute->builder ."' for attribute '" . $self->attribute->name . "'";
}

1;