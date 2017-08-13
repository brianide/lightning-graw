package ws.temple.graw.svn;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNDate;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

public class SVNRevisionFormatter {
	private static final Logger LOG = LoggerFactory.getLogger(SVNRevisionFormatter.class);
	
	private final Handlebars handlebars = new Handlebars();
	
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss z";
	public static final String DEFAULT_MESSAGE_FORMAT = "**[{{auth}}]** *(r{{rnum}})* @ {{date}}```{{body}}```";
	
	private DateFormat dateFormat;
	private Template messageFormat;
	
	public SVNRevisionFormatter(String dateFormat, String messageFormat) throws IOException {
		setDateFormat(dateFormat);
		setMessageFormat(messageFormat);
	}
	
	public SVNRevisionFormatter() {
		setDateFormat(DEFAULT_DATE_FORMAT);
		try {
			setMessageFormat(DEFAULT_MESSAGE_FORMAT);
		}catch (IOException e) {
			LOG.error("Unable to instantiate default formatter", e);
		}
	}
	
	public SVNRevisionFormatter setDateFormat(String dateFormat) {
		this.dateFormat = new SimpleDateFormat(dateFormat);
		return this;
	}
	
	public SVNRevisionFormatter setMessageFormat(String messageFormat) throws IOException {
		this.messageFormat = handlebars.compileInline(messageFormat);
		return this;
	}

	public String format(long rev, SVNProperties props) throws SVNException, IOException {
		final Map<String,String> map = new HashMap<>();
		map.put("auth", props.getStringValue(SVNRevisionProperty.AUTHOR));
		map.put("date", dateFormat.format(SVNDate.parseDateString(props.getStringValue(SVNRevisionProperty.DATE))));
		map.put("rnum", Long.toString(rev));
		
		// Check to make sure some asshole didn't commit without a message,
		// as Graw will vomit backticks in disgust if he sees such a thing.
		final String body = props.getStringValue(SVNRevisionProperty.LOG);
		map.put("body", body.isEmpty() ? "[No description]" : body);
		
		return StringEscapeUtils.unescapeHtml4(messageFormat.apply(Context.newContext(map)));
	}

}
