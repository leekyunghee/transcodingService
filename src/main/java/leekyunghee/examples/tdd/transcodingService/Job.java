package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.List;

public class Job {
	
	private MediaSourceFile mediaSourceFile;
	
	public Job(Long id, MediaSourceFile mediaSourceFile) {
		this.id = id;
		this.mediaSourceFile = mediaSourceFile;
	}
	
	private Long id;
	private State state;
	
	
	public void transcode(Transcoder transcoder, ThumbnailExtractor thumbnailExtractor,
            CreatedFileSender createdFileSender,
            JobResultNotifier jobResultNotifier) {
		try {
			changeState(Job.State.MEDIASOURCECOPYING);
			// 미디어 원본으로 부터 파일을 로컬에 복사한다. 
			File multimediaFile = copyMultimediaSourceToLocal();
			// 로컬에 복사된 파일을 변환처리 한다. 
			changeState(Job.State.TRANSCODER);
			List<File> multimediaFiles = transcode(multimediaFile, transcoder);
			// 로컬에 복사된 파일로부터 이미지를 추출한다. 
			changeState(Job.State.THUMBNAILEXTRACTOR);
			List<File> thumbnails = extractThumbnails(multimediaFile, thumbnailExtractor);
			// 변환된 결과 파일과 썸네일 이미지를 목적지에 저장 
			changeState(Job.State.CREATEDFILESENDOR);
			sendCreateFilesToDestination(multimediaFiles, thumbnails, createdFileSender);
			// 결과를 통지 
			changeState(Job.State.JOBRESULTNOTIFIER);
			notifyJobResultToRequester(jobResultNotifier);
			changeState(Job.State.COMPLETED);
			

		} catch(RuntimeException ex) {
			exceptionOccurred(ex);
			throw ex;
		}
	}

//	private void changeJobState(State newJobState) {
//		jobStateChanger.changeJobState(jobId, newJobState);
//	}

	private void notifyJobResultToRequester(JobResultNotifier jobResultNotifier) {
			jobResultNotifier.notifyToRequester(id);
	}

	private void sendCreateFilesToDestination(List<File> multimediaFiles, List<File> thumbnails, CreatedFileSender createdFileSender) {
			createdFileSender.store(multimediaFiles, thumbnails, id);
	}

	private List<File> extractThumbnails(File multimediaFile, ThumbnailExtractor thumbnailExtractor) {
			return thumbnailExtractor.extract(multimediaFile, id);
	}

	private List<File> transcode(File multimediaFile, Transcoder transcoder) {
			return transcoder.transcode(multimediaFile, id);
	}

	private File copyMultimediaSourceToLocal() {
			return mediaSourceFile.getSourceFile();
	}

	public static enum State {
		MEDIASOURCECOPYING, COMPLETED, TRANSCODER, CREATEDFILESENDOR, JOBRESULTNOTIFIER, THUMBNAILEXTRACTOR
	}
	private Exception occurredException;
	
	public boolean isSuccess() {
		return state == State.COMPLETED;
	}
	public State getLastState() {
		return this.state;
	}
	private void changeState(State newState) {
		this.state = newState;
	}
	public boolean isFinished() {
		return isSuccess() || isExceptionOccurred();
	}
	private boolean isExceptionOccurred() {
		return occurredException != null;
	}
	public Exception getOccurredException() {
		return occurredException;
	}
	private void exceptionOccurred(RuntimeException ex) {
		occurredException = ex;
	}
	public boolean isWaiting() {
		return state == null;
	}
	
}