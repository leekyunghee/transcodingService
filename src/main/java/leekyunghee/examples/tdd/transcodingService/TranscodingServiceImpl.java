package leekyunghee.examples.tdd.transcodingService;

public class TranscodingServiceImpl implements TranscodingService {

	private Transcoder transcoder;
	private CreatedFileSender createdFileSender;
	private ThumbnailExtractor thumbnailExtractor;
	private JobResultNotifier jobResultNotifier;
	private JobRepository jobRepository;
	
	public TranscodingServiceImpl(Transcoder transcoder, ThumbnailExtractor thumbnailExtractor,
		CreatedFileSender createdFileSender,
		JobResultNotifier jobResultNotifier, JobRepository jobRepository) {
		
		this.transcoder = transcoder;
		this.thumbnailExtractor = thumbnailExtractor;
		this.createdFileSender = createdFileSender;
		this.jobResultNotifier = jobResultNotifier;
		this.jobRepository = jobRepository;
	}

	
	@Override
	public void transcode(Long jobId) {
		Job job = jobRepository.findById(jobId);
		job.transcode(transcoder, thumbnailExtractor, createdFileSender, jobResultNotifier);
	}
}
