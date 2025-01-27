package com.petshop.petshop.service;

import com.petshop.petshop.DTO.ApiResponseDTO;
import com.petshop.petshop.exception.ResourceNotFoundException;
import com.petshop.petshop.model.Product;
import com.petshop.petshop.repository.ProductRepository;
import com.petshop.petshop.response.ApiResponseBuilder;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ApiResponseBuilder<List<Product>> listResponseBuilder;

    @Autowired
    private ApiResponseBuilder<Product> responseBuilder;

    @Override
    public ApiResponseDTO<List<Product>> getAllProducts() {
        return listResponseBuilder.createSuccessResponse(productRepository.findAll());
    }

    @Override
    public ApiResponseDTO<Product> getProduct(String id) {
        return  responseBuilder.createSuccessResponse(productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id)));
    }

    @Override
    public ApiResponseDTO<Product> createProduct(Product requestNewProduct, MultipartFile image) {
        if (productRepository.existsByName(
                requestNewProduct.getName().trim().replace("\\s+", ""))) {
            throw new ValidationException("Já existe um produto de mesmo nome cadastrado no sistema");
        }
        String imageUrl = imageService.saveImageToServer(image);
        if (imageUrl != null) {
            requestNewProduct.setImageUrl(imageUrl);
        }

        return responseBuilder.createSuccessResponse(productRepository.save(requestNewProduct));
    }

    @Override
    public ApiResponseDTO<Product> updateProduct(String id,
                                                 Product requestUpdateProduct, MultipartFile image) {
        return responseBuilder.createSuccessResponse(productRepository.findById(id)
                .map(product -> {
                    product.setName(requestUpdateProduct.getName());
                    product.setCategories(requestUpdateProduct.getCategories());
                    product.setUnitPrice(requestUpdateProduct.getUnitPrice());
                    product.setUnitsInStock(requestUpdateProduct.getUnitsInStock());
                    if (image != null && !image.isEmpty()) {
                        imageService.deleteImageFromServer(product.getImageUrl());
                        product.setImageUrl(imageService.saveImageToServer(image));
                    }
                    if (requestUpdateProduct.getImageUrl().isEmpty()){
                        imageService.deleteImageFromServer(product.getImageUrl());
                        product.setImageUrl(null);
                    }

                    return productRepository.save(product);
                }).orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id))
        );

    }

    @Override
    public void deleteProduct(String id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            imageService.deleteImageFromServer(product.get().getImageUrl());
            productRepository.deleteById(id);
        }
    }

}
