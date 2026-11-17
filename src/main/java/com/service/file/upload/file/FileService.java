package com.service.file.upload.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final FileRepository fileRepository;

    public void saveAll(List<File> files) {
        if (!files.isEmpty()) {
            fileRepository.saveAll(files);
        }
    }

    public List<File> findByStatus(FileStatus status, Limit limit) {
        return fileRepository.findByStatus(status, limit);
    }
}