package com.yrek.incant.gamelistings;

import android.util.Log;
import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class XMLScraper {
    public interface Handler {
        public void startDocument();
        public void endDocument();
        public void element(String path, String value);
    }

    private class Stack {
        final Stack parent;
        final String path;
        final StringBuffer value = new StringBuffer();

        Stack(Stack parent, String element) {
            this.parent = parent;
            this.path = parent == null ? element : (parent.path + "/" + element);
        }
    }

    private final SAXParser parser;
    private final DefaultHandler defaultHandler;

    public XMLScraper(final Handler handler) throws Exception {
        this.parser = SAXParserFactory.newInstance().newSAXParser();
        this.defaultHandler = new DefaultHandler() {
            Stack stack = null;
            @Override public void startDocument() {
                stack = null;
                handler.startDocument();
            }
            @Override public void endDocument() {
                stack = null;
                handler.endDocument();
            }
            @Override public void startElement(String uri, String localName, String qName, Attributes attributes) {
                stack = new Stack(stack, localName);
            }
            @Override public void endElement(String uri, String localName, String qName) {
                handler.element(stack.path, stack.value.toString());
                stack = stack.parent;
            }
            @Override public void characters(char[] ch, int start, int length) {
                stack.value.append(ch, start, length);
            }
        };
    }

    public void scrape(InputStream in) throws Exception {
        parser.parse(in, defaultHandler);
    }

    public void scrape(String url) throws Exception {
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            boolean retry = false;
            try {
                android.util.Xml.parse(in, Xml.Encoding.UTF_8, defaultHandler);
            } catch (Exception e) {
                Log.w("[XMLParseA] XMLScraper", "failed url " + url, e);
                retry = true;
            }

            if (retry) {
                in.close();
                in = new URL(url).openStream();
                retry = false;
                try {
                    android.util.Xml.parse(in, Xml.Encoding.ISO_8859_1, defaultHandler);
                    Log.w("XMLScraper", "[XMLParseA] retry worked! url " + url);
                } catch (Exception e) {
                    Log.w("XMLScraper", "[XMLParseA] failed 2nd method url " + url, e);
                    retry = true;
                }
            }


            // Exception problems on unicode links such as http://ifdb.tads.org/dladviser?xml&os=MacOSX&id=4z9yijaspsxbhfep
            // parser.parse(in, defaultHandler);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public void scrape(File file) throws Exception {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            parser.parse(in, defaultHandler);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
