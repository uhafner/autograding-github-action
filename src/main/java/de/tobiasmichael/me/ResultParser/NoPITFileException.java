package de.tobiasmichael.me.ResultParser;

import de.tobiasmichael.me.GithubComment.Commenter;
import edu.hm.hafner.analysis.Report;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * This class handles the case when there is no PIT file to parse.
 */
public class NoPITFileException extends FileNotFoundException {

    public NoPITFileException(String errorMessage, List<Report> reportList) {
        Commenter commenter = new Commenter(errorMessage, reportList);
        commenter.commentTo();
    }

}
