#!/bin/bash
cd empty/
./generate.sh
cd ../file_header/
./generate.sh
cd ../single_delta_encoding/
./generate.sh
cd ../double_delta_encoding/
./generate.sh
cd ../with_data/
./generate.sh