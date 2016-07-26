package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.List;

import leekyunghee.examples.tdd.transcodingService.Job.State;

public class TranscodingServiceImpl implements TranscodingService {

	private MediaSourceCopier mediaSourceCopier;
	private Transcoder transcoder;
	private CreatedFileSender createdFileSender;
	private ThumbnailExtractor thumbnailExtractor;
	private JobResultNotifier jobResultNotifier;
	private JobStateChanger jobStateChanger;
	private TranscodingExceptionHandler transcodingExceptionHandler;
	
	public TranscodingServiceImpl(MediaSourceCopier mediaSourceCopier,
		Transcoder transcoder, ThumbnailExtractor thumbnailExtractor,
		CreatedFileSender createdFileSender,
		JobResultNotifier jobResultNotifier, JobStateChanger jobStateChanger, TranscodingExceptionHandler transcodingExceptionHandler) {
		
		this.mediaSourceCopier = mediaSourceCopier;
		this.transcoder = transcoder;
		this.thumbnailExtractor = thumbnailExtractor;
		this.createdFileSender = createdFileSender;
		this.jobResultNotifier = jobResultNotifier;
		this.jobStateChanger = jobStateChanger;
		this.transcodingExceptionHandler = transcodingExceptionHandler;
	}

	public void transcode(Long jobId) {
		changeJobState(jobId, Job.State.MEDIASOURCECOPYING);
		// 미디어 원본으로 부터 파일을 로컬에 복사한다. 
		File multimediaFile = copyMultimediaSourceToLocal(jobId);
		// 로컬에 복사된 파일을 변환처리 한다. 
		List<File> multimediaFiles = transcode(multimediaFile, jobId);
		// 로컬에 복사된 파일로부터 이미지를 추출한다. 
		List<File> thumbnails = extractThumbnails(multimediaFile, jobId);
		// 변환된 결과 파일과 썸네일 이미지를 목적지에 저장 
		sendCreateFilesToDestination(multimediaFiles, thumbnails, jobId);
		// 결과를 통지 
		notifyJobResultToRequester(jobId);
		changeJobState(jobId, Job.State.COMPLETED);
	}

	private void changeJobState(Long jobId, State newJobState) {
		jobStateChanger.changeJobState(jobId, newJobState);
	}

	private void notifyJobResultToRequester(Long jobId) {
		jobResultNotifier.notifyToRequester(jobId);
		
	}

	private void sendCreateFilesToDestination(List<File> multimediaFiles, List<File> thumbnails, Long jobId) {
		createdFileSender.send(multimediaFiles, thumbnails, jobId);
	}

	private List<File> extractThumbnails(File multimediaFile, Long jobId) {
		return thumbnailExtractor.extract(multimediaFile, jobId);
	}

	private List<File> transcode(File multimediaFile, Long jobId) {
		return transcoder.transcode(multimediaFile, jobId);
	}

	private File copyMultimediaSourceToLocal(Long jobId) {
		try {
			return mediaSourceCopier.copy(jobId);
		}catch(RuntimeException ex) {
			transcodingExceptionHandler.notifyToJob(jobId, ex);
			throw ex;
		}
	}
}
