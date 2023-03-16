#!/bin/bash
# script to delete a Collection and all its Files from Solr
# Usage: ./delete_dataset.sh <collection_id>

collection_id="$1"
if [ "$collection_id" == "" ]; then
  echo "Usage: ./delete_collection.sh <collection_id>"
  exit
fi

echo "Deleting Files for CollectionId=$collection_id"
wget --no-check-certificate "https://localhost:8984/solr/files/update?stream.body=<delete><query>CollectionId:$collection_id</query></delete>&commit=true"

echo "Deleting Datasets for CollectionId=$collection_id"
wget --no-check-certificate "https://localhost:8984/solr/datasets/update?stream.body=<delete><query>CollectionId:$collection_id</query></delete>&commit=true"

echo "Deleting Collection for id=$collection_id"
wget --no-check-certificate "https://localhost:8984/solr/collections/update?stream.body=<delete><query>id:$collection_id</query></delete>&commit=true"
