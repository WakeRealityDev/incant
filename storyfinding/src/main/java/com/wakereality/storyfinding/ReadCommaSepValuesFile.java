package com.wakereality.storyfinding;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Created by Stephen A Gutknecht on 4/1/17.
 * on Linux, this produces CSV file that has all CRLF intact and proper escaping
 *   mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT * FROM games WHERE system LIKE '%Inform%' AND author='Emily Short'" | awk -F'\t' '{ sep=""; for(i = 1; i <= NF; i++) { gsub(/\\t/,"\t",$i); gsub(/\\n/,"\n",$i); gsub(/\\\\/,"\\",$i); gsub(/"/,"\"\"",$i); printf sep"\""$i"\""; sep=","; if(i==NF){printf"\n"}}}'
 * awk tip here:
 *    http://stackoverflow.com/questions/15640287/change-output-format-for-mysql-command-line-results-to-csv/17910254#17910254
 *
 * An quick way to view the table structures
 *     cat ifdb-archive.sql | less -S
 *
 * Star ratings:
 *   mysql --login-path=wakedev -ss --database=ifdbarchive --batch --skip-column-names -e "SELECT a.id, AVG(rating) AS AVGrating, COUNT(rating) AS COUNTrating FROM games AS a INNER JOIN reviews AS r ON a.id = r.gameid  GROUP BY id HAVING COUNTrating > 2 ORDER BY AVGrating DESC;" |  sed 's/\t/,/g' > ifdb_stars_list0.csv
 *
 */

public class ReadCommaSepValuesFile {

    public static String rc(final String inString) {
        return inString.replace(",", "&");
    }

    public void tryCSV0(Context context) {
        String next[] = {};
        List<String[]> list = new ArrayList<String[]>();
        try {
            String fileSource = "csv/tryout.csv";
            fileSource = "csv/many_inform_list0.csv";
            CSVReader reader = new CSVReader(new InputStreamReader(context.getAssets().open(fileSource)));
            for(;;) {
                next = reader.readNext();
                if (next != null) {
                    list.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("ReadCSV", "[ReadCSV] got " + list.size());
        for (int i = 0; i < list.size(); i++) {
            if (i % 10 == 0) {
                String[] e = list.get(i);
                Log.i("ReadCSV", "[ReadCSV] # " + i + ": " + rc(e[1]) + ", " + rc(e[2]));
            }
        }

    }
}
