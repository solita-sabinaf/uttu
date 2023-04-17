#!/bin/bash  -e

for f in ./db/migration/V1__Base_version.sql \
           ./db/migration/V2__Add_Export_filename.sql \
           ./db/migration/V3__Remove_unique_order_constraints.sql \
           ./db/migration/V4__Increased_text_column_length.sql \
           ./db/migration/V5__Increased_booking_note_text_column_length.sql \
           ./db/migration/V6__Add_simple_line.sql \
           ./db/migration/V7__Add_export_line_associations.sql \
           ./db/migration/V8__On_delete_cascade_line_fkey_in_export_line_association.sql \
           ./db/migration/V9__Add_key_list.sql \
           ./db/migration/V10__Drop_fromDate_to_Date_on_Export.sql \
           ./db/migration/V11__Add_exported_line_statistics.sql \
           ./db/migration/V12__Add_service_journey_name.sql \
           ./db/migration/V13__Day_type_Service_journey_many_to_many.sql
do
  echo "Running migration for ${f}"
  PGPASSWORD=uttu psql -U uttu -h localhost -p 5432 -f $f
done
