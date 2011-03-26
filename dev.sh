#!/bin/sh

cd src/piaget

emacs &

cd ../..

lein swank
