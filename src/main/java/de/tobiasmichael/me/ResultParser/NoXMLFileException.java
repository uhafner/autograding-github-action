package de.tobiasmichael.me.ResultParser;

import de.tobiasmichael.me.GithubComment.Commenter;

import java.io.FileNotFoundException;

public class NoXMLFileException extends FileNotFoundException {

    public NoXMLFileException(String errorMessage, Throwable err) {
        Commenter commenter = new Commenter(errorMessage, err);
        commenter.commentTo();
    }

}
