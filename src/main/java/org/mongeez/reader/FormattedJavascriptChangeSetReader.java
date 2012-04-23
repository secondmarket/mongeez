/*
 * Copyright 2011 SecondMarket Labs, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.mongeez.reader;

import org.mongeez.commands.ChangeSet;
import org.mongeez.commands.Script;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormattedJavascriptChangeSetReader implements ChangeSetReader {
    private static final String LINE_COMMENT = "//";
    private static final String FILE_HEADER = "mongeez formatted javascript";
    private static final Pattern FILE_HEADER_PATTERN =
            Pattern.compile("//\\s*mongeez\\s+formatted\\s+javascript\\s*",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANGESET_PATTERN =
            Pattern.compile("//\\s*changeset\\s+([\\w\\-]+):([\\w\\-]+).*",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_RUN_ALWAYS_PATTERN =
            Pattern.compile(".*runAlways:(\\w+).*",
                    Pattern.CASE_INSENSITIVE);

    private final Charset cs;

    public FormattedJavascriptChangeSetReader() {
        this(Charset.forName("UTF-8"));
    }

    public FormattedJavascriptChangeSetReader(Charset cs) {
        this.cs = cs;
    }

    @Override
    public boolean supports(Resource file) {
        if (file.getFilename().endsWith(".js")) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        file.getInputStream()));
                String line = reader.readLine();
                return line != null && FILE_HEADER_PATTERN.matcher(line).matches();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignore) {}
                }
            }
        }
        return false;
    }

    @Override
    public List<ChangeSet> getChangeSets(Resource file) {
        List<ChangeSet> changeSets = new ArrayList<ChangeSet>();

        try {
            changeSets.addAll(parse(file));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return changeSets;
    }

    private List<ChangeSet> parse(Resource file) throws IOException, ParseException {
        List<ChangeSet> changeSets = new ArrayList<ChangeSet>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                file.getInputStream(), cs));
        try {
            String line = reader.readLine();
            parseFileHeader(file, line);
            ChangeSet changeSet = null;
            StringBuilder scriptBody = null;
            line = reader.readLine();
            while (line != null) {
                ChangeSet newChangeSet = parseChangeSetStart(line);
                if (newChangeSet != null) {
                    addScriptToChangeSet(changeSet, scriptBody);
                    changeSet = newChangeSet;
                    scriptBody = new StringBuilder();
                    changeSet.setFile(file.getFilename());
                    changeSets.add(changeSet);
                } else {
                    scriptBody.append(line);
                    scriptBody.append('\n');
                }
                line = reader.readLine();
            }
            addScriptToChangeSet(changeSet, scriptBody);
        } finally {
            try {
                reader.close();
            } catch(IOException ignore) {}
        }
        return changeSets;
    }

    private void addScriptToChangeSet(ChangeSet changeSet, StringBuilder scriptBody) throws ParseException {
        if (changeSet != null) {
            String body = scriptBody.toString();
            if (body.trim().isEmpty()) {
                throw new ParseException("No JavaScript found for changeset " + toString(changeSet), -1);
            }
            Script script = new Script();
            script.setBody(body);
            changeSet.add(script);
        }
    }

    private String toString(ChangeSet changeSet) {
        return changeSet.getAuthor() + ":" + changeSet.getChangeId();
    }

    private void parseFileHeader(Resource file, String line) throws IOException, ParseException {
        if (line == null || !FILE_HEADER_PATTERN.matcher(line).matches()) {
            throw new ParseException(file.getFile().getPath() +
                    " did not begin with the expected comment:\n" +
                    LINE_COMMENT + FILE_HEADER, -1);
        }
    }

    private ChangeSet parseChangeSetStart(String line) {
        ChangeSet changeSet = null;
        Matcher changeSetMatcher = CHANGESET_PATTERN.matcher(line);
        if (changeSetMatcher.matches()) {
            changeSet = new ChangeSet();
            changeSet.setAuthor(changeSetMatcher.group(1));
            changeSet.setChangeId(changeSetMatcher.group(2));
            changeSet.setRunAlways(parseAttribute(ATTRIBUTE_RUN_ALWAYS_PATTERN.matcher(line), false));
        }
        return changeSet;
    }

    private boolean parseAttribute(Matcher attributeMatcher, boolean defaultValue) {
        boolean attributeValue = defaultValue;
        if (attributeMatcher.matches()) {
            attributeValue = Boolean.parseBoolean(attributeMatcher.group(1));
        }
        return attributeValue;
    }

}