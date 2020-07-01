package de.tobiasmichael.me.ResultParser;

import de.tobiasmichael.me.GithubComment.Commenter;

import java.io.FileNotFoundException;

public class NoPITFileException extends FileNotFoundException {

    public NoPITFileException(String errorMessage, Throwable err) {
        Commenter commenter = new Commenter(errorMessage, err);
        commenter.commentTo();
    }

}
