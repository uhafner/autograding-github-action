package de.tobiasmichael.me.ResultParser;

import de.tobiasmichael.me.GithubComment.Commenter;

import java.io.FileNotFoundException;
import java.io.IOException;

public class NoXMLFileException extends FileNotFoundException {

    public NoXMLFileException(String errorMessage, Throwable err) throws IOException {
        Commenter commenter = new Commenter(errorMessage, err);
        commenter.commentTo();
    }

}
